@file:JvmName("MiscellaneousUtils")
@file:JvmMultifileClass
@file:Suppress("DEPRECATED_JAVA_ANNOTATION") //Used for notifying the Java-code of removal.
/**
 * @file
 * This contains all the deprecated utils that were ported over from JDA Command System.
 * They are reworked to still work under Catnip and to be backwards compatible with JDA Command System.
 * However, it isn't guaranteed that any of these methods or fields will stay.
 *
 * Any method marked with a java.lang.Deprecated(forRemoval = true) will be removed quickly.
 * Methods marked with java.lang.Deprecated(forRemoval = false) or missing this may be into consideration of keeping.
 *
 * Although, some methods are marked as WARNING, but that's just to allow suppression of errors.
 * */
package net.kjp12.commands.utils

import com.mewna.catnip.entity.channel.*
import com.mewna.catnip.entity.message.Message
import com.mewna.catnip.entity.message.MessageOptions
import com.mewna.catnip.entity.util.Permission
import java.util.function.Consumer


@java.lang.Deprecated(forRemoval = true)
@Deprecated("JDA leftover; Glorified failback resolver; Cannot pass the fail consumer over.", level = DeprecationLevel.WARNING /*Should be #ERROR, but Kotlin complains due to #sendMessage(Message?, Consumer<MessageChannel>)*/)
fun Message?.sendMessage(sendTo: Consumer<MessageChannel>, fail: Consumer<Throwable?>?) {
    if(this != null) try {
        val mc = channel()
        if(mc is DMChannel || mc is GroupDMChannel) return sendTo.accept(mc)
        if(mc is TextChannel)
            if(catnip().cache().member(guild()!!.id(), catnip().selfUser()!!.id())?.hasPermissions(mc, Permission.SEND_MESSAGES) ?: throw AssertionError("Guild Member of Self not found"))
                return sendTo.accept(mc)
        author().createDM().thenAcceptAsync { sendTo.accept(it) }
    } catch (t: Throwable) {
        fail?.accept(t)
    } else fail?.accept(null)
}

@Suppress("DeprecatedCallableAddReplaceWith") //redirect to a deprecated bound for removal.
@java.lang.Deprecated(forRemoval = true)
@Deprecated("JDA leftover; Glorified failback resolver", level = DeprecationLevel.ERROR)
fun Message?.sendMessage(sendTo: Consumer<MessageChannel>) = sendMessage(sendTo, null)


//TODO: Bring back in new form?
@Suppress("DeprecatedCallableAddReplaceWith") //IntelliJ bug; shouldn't require suppression
@java.lang.Deprecated(forRemoval = true)
@Deprecated("JDA leftover; Glorified permission test.", level = DeprecationLevel.WARNING /*Should be #ERROR*/)
fun <O> Message.properMessage(embed: (MessageChannel) -> O, text: (MessageChannel) -> O): O = (if(channel() is GuildChannel || (catnip().cache().member(guildId()!!, catnip().selfUser()!!.id())?.hasPermissions(channel() as GuildChannel, Permission.EMBED_LINKS) ?: throw AssertionError("Guild Member of Self not found"))) embed else text)(channel())

//TODO: Bring back in new form?
@java.lang.Deprecated(forRemoval = true)
@Deprecated("JDA leftover; Glorified permission test; Blocking!!! Massive error when using in event loop", level = DeprecationLevel.ERROR)
fun <O> Message.properMessage(embed: (MessageChannel) -> O, text: (MessageChannel) -> O, vararg requiredElseDM: Permission): O {
    if(requiredElseDM.isEmpty()) return properMessage(embed, text)
    val channel = channel()
    if(channel is GuildChannel) {
        val self = catnip().cache().member(guildId()!!, catnip().selfUser()!!.id()) ?: throw AssertionError("Guild Member of Self not found")
        return if(self.hasPermissions(channel, requiredElseDM.toList())) embed(author().createDM().toCompletableFuture().join()) /*TODO: Throw instead due to blocking nature?*/ else properMessage(embed, text)
    }
    return embed(channel)
}

@java.lang.Deprecated(forRemoval = true)
@Deprecated("JDA leftover", ReplaceWith("MessageOptions().attachString(name, content)", "com.mewna.catnip.entity.message.MessageOptions"), DeprecationLevel.ERROR)
fun attachString(deprecated: MessageChannel, name: String, content: String) = MessageOptions().attachString(name, content)

@Deprecated("Leftover", ReplaceWith("this.first()"), DeprecationLevel.ERROR)
val <T> Iterable<T>.first
    @java.lang.Deprecated(forRemoval = true)
    @JvmName("getFirst")
    get() = first()

@java.lang.Deprecated(forRemoval = true)
@Deprecated("Unnecessary", level = DeprecationLevel.ERROR)
fun <I> I.distribute(vararg cArr: Consumer<I>): I {
    for(c in cArr) c.accept(this)
    return this //fluent designs
}