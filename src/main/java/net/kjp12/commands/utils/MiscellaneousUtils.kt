@file:JvmName("MiscellaneousUtils")

package net.kjp12.commands.utils

import com.mewna.catnip.cache.view.CacheView
import com.mewna.catnip.entity.builder.EmbedBuilder
import com.mewna.catnip.entity.channel.DMChannel
import com.mewna.catnip.entity.channel.MessageChannel
import com.mewna.catnip.entity.channel.Webhook
import com.mewna.catnip.entity.guild.Guild
import com.mewna.catnip.entity.guild.Member
import com.mewna.catnip.entity.message.Embed
import com.mewna.catnip.entity.message.Message
import com.mewna.catnip.entity.message.MessageOptions
import com.mewna.catnip.entity.user.User
import com.mewna.catnip.entity.util.ImageOptions
import com.mewna.catnip.entity.util.ImageType
import com.mewna.catnip.entity.util.Permission
import com.mewna.catnip.entity.util.Permission.*
import com.mewna.catnip.util.Utils
import io.reactivex.Single
import net.kjp12.commands.abstracts.ICommand
import net.kjp12.commands.abstracts.ICommandListener
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.OffsetDateTime
import java.time.temporal.TemporalAccessor
import java.util.*
import java.util.concurrent.Callable
import java.util.function.Supplier

private val LOGGER = LoggerFactory.getLogger("MiscellaneousUtils")
const val MASK_32 = 0xFFFFFFFFL
const val MASK_16 = 0xFFFFL

fun genBaseEmbed(colour: Int, author: Any?, g: Guild?, title: String?, footer: Any?, time: TemporalAccessor): EmbedBuilder {
    val eb = EmbedBuilder().color(
        g?.selfMember()?.color()?.rgb ?: if (author is Member) author.color()?.rgb ?: g?.roles()?.random?.color()
        ?: colour else colour
    ).timestamp(time)
    when (author) {
        is User -> eb.author("${author.username()}#${author.discriminator()} ${author.id()}", author.avatar, author.avatar)
        is Member -> {
            val author = author.catnip().cache().user(author.id()) ?: throw InternalError("Member $author - ${author.id()} known, yet cannot find user instance?!")
            eb.author("${author.username()}#${author.discriminator()} ${author.id()}", author.avatar, author.avatar)
        }
        is Embed.Author -> eb.author(author)
    }
    var footer = footer
    if (title != null) eb.title(title)
    if (footer == null && g != null) footer = g.catnip().selfUser()
    if (footer != null) when (footer) {
        is Member -> {
            val u = footer.user()
            eb.footer(u.username(), u.avatar)
        }
        is User -> eb.footer(footer.username(), footer.avatar)
        is Embed.Footer -> eb.footer(footer)
        else -> eb.footer(footer.toString(), null)
    }
    return eb
}

fun attemptSend(listener: ICommandListener, t: Throwable?, msg: Message?): UUID {
    //now, we won't always have access to a message; we must generate it.
    val tmp = (t?.hashCode() ?: 0).toLong()
    var msb = tmp shr 16 xor tmp and MASK_16 shl 16
    val lsb: Long
    if (msg == null) {
        msb = msb or (System.currentTimeMillis() / 1000L and MASK_32 shl 32 or listener.hashCode().toLong())
        lsb = listener.hashCode().toLong() or (tmp shl 32)
    } else {
        msb = msb or (msg.idAsLong().ushr(22) + Utils.DISCORD_EPOCH shl 32)
        val g = msg.guild()
        val shard = msg.catnip().shardManager().shardCount().toLong()
        lsb = msg.idAsLong() or (shard and (MASK_16 shl 32)) or if (g == null) 0 else (g.idAsLong() shr 22) % shard and MASK_16 shl 48
    }
    val uid = UUID(msb, lsb)
    if (t != null) {
        try {
            sendError(listener.webhook, t, uid, msg)
        } catch (ioe: Throwable) {
            LOGGER.error("Couldn't send error {}!", t, ioe)
        }

        LOGGER.error("Error! {}", uid, t)
    }
    return uid
}

fun sendError(wc: Webhook?, stack: Throwable, uid: UUID, msg: Message?) {
    if (wc == null) return
    val eb = EmbedBuilder().color(0xAA1200).title("Something has failed").footer("Error UID - $uid", null).timestamp(now)
    if (msg != null) {
        val auth = msg.author()
        val guild = msg.guild()
        val edit = msg.editedTimestamp()
        eb.author(auth.username() + '#'.toString() + auth.discriminator() + " " + auth.id(), null, auth.avatar).description(msg.content())
                .field("Message Information", """${if (guild != null) "**Guild** ➠ ${guild.name()} ${guild.id()}\n" else ""}**Channel** ➠ <#${msg.channelId()}> ${msg.channelId()}-${msg.id()}
**Posted** ➠ ${msg.creationTime()}${if (edit != null) "\n**Edited** ➠ $edit" else ""}""", true)
    }

    val sw = StringWriter()
    val pw = PrintWriter(sw)
    stack.printStackTrace(pw)
    wc.executeWebhook(MessageOptions().embed(eb.build()).attachString("$uid-stack", sw.toString()))
}

fun MessageOptions.attachString(name: String, content: String) = addFile(name, content.toByteArray(StandardCharsets.UTF_8))

fun ICommandListener.getStackedPrefix(guild: Guild?): String {
    val sb = StringBuilder()
    var icl = this
    while (icl is ICommand) {
        sb.insert(0, ' ').insert(0, icl.firstAliases)
        icl = icl.listener
    }
    return sb.insert(0, icl.getPrefix(guild)).toString()
}

fun Message.canEmbed() = !channel().isGuild || guild()!!.selfMember().hasPermissions(channel().asGuildChannel(), EMBED_LINKS)

fun Message?.selfHasPermissions(perm: Permission) = selfHasPermissions(EnumSet.of(perm))

fun Message?.selfHasPermissions(first: Permission, vararg rest: Permission) = selfHasPermissions(EnumSet.of(first, *rest))

fun Message?.selfHasPermissions(permissions: EnumSet<Permission>) = this != null && (guild()?.selfMember()?.hasPermissions(channel().asGuildChannel(), permissions) ?: !guildRequiredPermissions.any { permissions.contains(it) })

@JvmOverloads
fun Message?.getSendableChannel(options: MessageOptions? = null): Single<out MessageChannel> {
    val e = options?.embed() != null
    val a = options?.hasFiles() == true
    return getSendableChannel(
        if (e && a) EnumSet.of(VIEW_CHANNEL, SEND_MESSAGES, ATTACH_FILES, EMBED_LINKS)
        else if (e) EnumSet.of(VIEW_CHANNEL, SEND_MESSAGES, EMBED_LINKS)
        else if (a) EnumSet.of(VIEW_CHANNEL, SEND_MESSAGES, ATTACH_FILES)
        else EnumSet.of(VIEW_CHANNEL, SEND_MESSAGES)
    )
}

fun Message?.getSendableChannel(first: Permission) = getSendableChannel(EnumSet.of(first))

fun Message?.getSendableChannel(first: Permission, vararg rest: Permission) = getSendableChannel(EnumSet.of(first, *rest))

fun Message?.getSendableChannel(permissions: EnumSet<Permission>): Single<out MessageChannel> = if (this == null) Single.error(NullPointerException("Message")) else if (selfHasPermissions(permissions)) Single.just(channel()) else author().dmChannel

fun <T> Callable<T>.getOrDefault(def: Supplier<T>) = try {
    call()
} catch (e: Exception) {
    def.get()
}

fun <T> Callable<T>.getOrDefault(t: T) = try {
    call()
} catch (e: Exception) {
    t
}

//<editor-fold desc="fields">
val User.avatar: String
    get() {
        val a = avatar()
        return effectiveAvatarUrl(ImageOptions().type(if (a == null || !a.startsWith("a_")) ImageType.PNG else ImageType.GIF).size(1024))
    }

val User.dmChannel: Single<DMChannel>
    get() {
        val cache = catnip().cache().dmChannels().getById(idAsLong())
        return if (cache != null) Single.just(cache) else createDM()
    }

val Member.dmChannel: Single<DMChannel>
    get() = user().dmChannel

val Message.dmChannel: Single<DMChannel>
    get() = if (channel().isDM) Single.just(channel().asDMChannel()) else author().dmChannel

val now: OffsetDateTime
    @JvmName("now") get() = OffsetDateTime.now(Clock.systemUTC())

val <T> CacheView<T>.random: T
    get() = values().random()

val guildRequiredPermissions: EnumSet<Permission>
    get() = EnumSet.of(CREATE_INSTANT_INVITE, KICK_MEMBERS, BAN_MEMBERS, ADMINISTRATOR, MANAGE_CHANNELS, MANAGE_GUILD, VIEW_AUDIT_LOG, MANAGE_MESSAGES, MUTE_MEMBERS, DEAFEN_MEMBERS, MOVE_MEMBERS, PRIORITY_SPEAKER, CHANGE_NICKNAME, MANAGE_NICKNAME, MANAGE_ROLES, MANAGE_WEBHOOKS, MANAGE_EMOJI)

val generalPermissions: EnumSet<Permission>
    get() = EnumSet.of(ADD_REACTIONS, VIEW_CHANNEL, SEND_MESSAGES, SEND_TTS_MESSAGES, EMBED_LINKS, ATTACH_FILES, READ_MESSAGE_HISTORY, USE_EXTERNAL_EMOJI)
//</editor-fold>