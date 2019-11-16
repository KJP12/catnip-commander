package net.kjp12.commands.utils;

import com.mewna.catnip.Catnip;
import com.mewna.catnip.entity.builder.EmbedBuilder;
import com.mewna.catnip.entity.channel.DMChannel;
import com.mewna.catnip.entity.channel.MessageChannel;
import com.mewna.catnip.entity.channel.Webhook;
import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.guild.Member;
import com.mewna.catnip.entity.guild.Role;
import com.mewna.catnip.entity.message.Embed;
import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.message.MessageOptions;
import com.mewna.catnip.entity.user.User;
import com.mewna.catnip.entity.util.ImageOptions;
import com.mewna.catnip.entity.util.ImageType;
import com.mewna.catnip.entity.util.Permission;
import io.reactivex.Single;
import net.kjp12.commands.abstracts.ICommandListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import static com.mewna.catnip.entity.util.Permission.*;


public final class MiscellaneousUtils {
    public static final EnumSet<Permission> guildRequiredPermissions =
            EnumSet.of(CREATE_INSTANT_INVITE, KICK_MEMBERS, BAN_MEMBERS, ADMINISTRATOR, MANAGE_CHANNELS, MANAGE_GUILD, VIEW_AUDIT_LOG, MANAGE_MESSAGES, MUTE_MEMBERS, DEAFEN_MEMBERS, MOVE_MEMBERS, PRIORITY_SPEAKER, CHANGE_NICKNAME, MANAGE_NICKNAME, MANAGE_ROLES, MANAGE_WEBHOOKS, MANAGE_EMOJI),
            generalPermissions =
                    EnumSet.of(ADD_REACTIONS, VIEW_CHANNEL, SEND_MESSAGES, SEND_TTS_MESSAGES, EMBED_LINKS, ATTACH_FILES, READ_MESSAGE_HISTORY, USE_EXTERNAL_EMOJI);
    public static final long MASK_32 = 0xFFFFFFFFL, MASK_16 = 0xFFFFL;
    public static final Random RANDOM = new Random();
    private static final Logger LOGGER = LoggerFactory.getLogger("MiscellaneousUtils");

    private MiscellaneousUtils() {
    }

    @Deprecated(forRemoval = true)
    public static EmbedBuilder genBaseEmbed(int colour, Object author, Guild guild, String title, Embed.Footer footer, TemporalAccessor time) {
        return genBaseEmbed(colour, author, footer, title, guild, time);
    }

    @Deprecated(forRemoval = true)
    public static EmbedBuilder genBaseEmbed(int colour, Object author, Guild guild, String title, Member footer, TemporalAccessor time) {
        return genBaseEmbed(colour, author, footer, title, guild, time);
    }

    @Deprecated(forRemoval = true)
    public static EmbedBuilder genBaseEmbed(int colour, Object author, Guild guild, String title, User footer, TemporalAccessor time) {
        return genBaseEmbed(colour, author, footer, title, guild, time);
    }

    /**
     * @param colour Default colour of the sidebar. -1 for default.
     * @param author Usually self. Can be {@link Embed.Author}, {@link Catnip}, {@link Member}, {@link User} and {@link CharSequence}.
     *               When {@link Member} and {@link User}, the output is {@code "User"} and will be used for colour fetching.
     *               A {@link ToIntFunction} maybe provided for custom initialisation. It is assumed that the type is {@link EmbedBuilder}.
     *               {@code null} will omit the field.
     *               Any other objects will use {@code .toString()}.
     * @param footer Usually runner. Can be {@link Embed.Footer}, {@link Catnip}, {@link Member}, {@link User} and {@link CharSequence}.
     *               When {@link Member} and {@link User}, the output is {@code "User#0000 196188877885538304"} and will be used for colour fetching.
     *               A {@link ToIntFunction} maybe provided for custom initialisation. It is assumed that the type is {@link EmbedBuilder}.
     *               {@code null} will omit the field.
     *               Any other objects will use {@code .toString()}.
     * @param title  What is it you're displaying?
     * @param guild  A server to fetch colours from.
     * @param time   Timestamp, usually of run.
     * @return An {@link EmbedBuilder} primed with your input.
     */
    public static EmbedBuilder genBaseEmbed(int colour, Object author, Object footer, String title, Guild guild, TemporalAccessor time) {
        return genBaseEmbed(colour, 0b1, author, footer, title, guild, time);
    }

    /**
     * @param flags Bitwise flags of operation.
     *              Bits 0-1 : Footer
     *              0 == KJP12
     *              1 || 3 == KJP12#3880 196188877885538304
     *              2 == KJP12#3880
     *              Bits 2-3 : Author
     *              0 == KJP12
     *              1 || 3 == KJP12#3880 196188877885538304
     *              2 == KJP12#3880
     *              Bits 4-4 : Colour
     *              1 == Default Colour
     * @see #genBaseEmbed(int, Object, Object, String, Guild, TemporalAccessor)
     */
    public static EmbedBuilder genBaseEmbed(int colour, int flags, Object author, Object footer, String title, Guild guild, TemporalAccessor time) {
        var eb = new EmbedBuilder().timestamp(time);
        int color = ((flags >> 4) & 1) == 1 ? colour : -1;
        if (author != null) {
            if (author instanceof Catnip) {
                author = ((Catnip) author).selfUser();
            }
            if (author instanceof Embed.Author) {
                eb.author((Embed.Author) author);
            } else if (author instanceof Member) {
                var m = (Member) author;
                var u = m.user();
                var a = avatar(u);
                if (color == -1) color = getColour(m);
                eb.author(u.username(), a, a);
            } else if (author instanceof User) {
                var u = (User) author;
                var a = avatar(u);
                if (color == -1) color = getColour(guild.member(u.idAsLong()));
                eb.author(u.username(), a, a);
            } else if (author instanceof ToIntFunction) {
                var f = (ToIntFunction) author;
                var c = f.applyAsInt(eb);
                if (color == -1) color = c;
            } else {
                eb.author(author.toString());
            }
        }
        if (footer != null) {
            if (footer instanceof Catnip) {
                footer = ((Catnip) footer).selfUser();
            }
            if (footer instanceof Embed.Footer) {
                eb.footer((Embed.Footer) footer);
            } else if (footer instanceof Member) {
                var m = (Member) footer;
                var u = m.user();
                var a = avatar(u);
                if (color == -1) color = getColour(m);
                eb.footer((flags & 1) == 1 ? (u.discordTag() + ' ' + u.idAsLong()) : ((flags >> 1) & 1) == 1 ? u.discordTag() : u.username(), a);
            } else if (footer instanceof User) {
                var u = (User) footer;
                var a = avatar(u);
                if (color == -1) color = getColour(guild.member(u.idAsLong()));
                eb.footer((flags & 1) == 1 ? (u.discordTag() + ' ' + u.idAsLong()) : ((flags >> 1) & 1) == 1 ? u.discordTag() : u.username(), a);
            } else if (footer instanceof ToIntFunction) {
                var f = (ToIntFunction) footer;
                var c = f.applyAsInt(eb);
                if (color == -1) color = c;
            } else if (footer != null) {
                eb.footer(footer.toString(), null);
            }
        }
        if (title != null && !title.isBlank()) eb.title(title);
        if (color == -1) {
            var v = guild.roles().values();
            if (!v.isEmpty()) {
                var i = v.iterator();
                Role r = i.next();
                if (i.hasNext()) for (int s = RANDOM.nextInt(v.size() - 1); s > 0 && i.hasNext(); s--, r = i.next()) ;
                colour = r.color();
            }
        }
        return eb.color(color == -1 ? colour : color);
    }

    public static UUID attemptSend(ICommandListener icl, Throwable t, Message msg) {
        long msb, lsb;
        if (msg == null) {
            msb = icl.hashCode() | (((System.currentTimeMillis() / 1000L) & MASK_32) << 32);
            lsb = Objects.hashCode(t);
        } else {
            msb = msg.idAsLong();
            lsb = (((long) icl.hashCode()) << 32) | msg.channelIdAsLong();
        }
        var uid = new UUID(msb, lsb);
        if (t != null) {
            try {
                sendError(icl.getWebhook(), t, uid, msg);
            } catch (IOException ioe) {
                t.addSuppressed(ioe);
            }
            LOGGER.error("Error caught!", t);
        }
        return uid;
    }

    public static void sendError(Webhook hook, Throwable stack, UUID uid, Message msg) throws IOException {
        if (hook == null) return;
        EmbedBuilder eb = null;
        if (msg != null) {
            var auth = msg.author();
            var guild = msg.guild();
            var edit = msg.editedTimestamp();
            eb = genBaseEmbed(0xAA1200, 0b10100, auth, "Error - " + uid, "Something has failed", guild, now()).description(msg.content());
            var sb = new StringBuilder();
            if (guild != null)
                sb.append("**Guild** ➠ ").append(guild.name()).append(' ').append(guild.idAsLong()).append('\n');
            sb.append("**Channel** ➠ <#").append(msg.channelIdAsLong()).append("> [").append(msg.channelIdAsLong()).append('-').append(msg.idAsLong()).append("](").append(getLink(msg)).append(')')
                    .append("\n**Posted** ➠ ").append(msg.creationTime());
            if (edit != null) sb.append("\n**Edited** ➠ " + edit);
            eb.field("Message Information", sb.toString(), true);
        }
        try (var sw = new StringWriter(); var pw = new PrintWriter(sw)) {
            stack.printStackTrace(pw);
            hook.executeWebhook(attachString(new MessageOptions().embed((eb != null ? eb : genBaseEmbed(0xAA1200, 0b10000, hook.catnip(), "Error - " + uid, "Something has failed", null, now())).build()), uid + "-stack", sw.toString()));
        }
    }

    /**
     * @deprecated Use {@link ICommandListener#getStackedPrefix(Guild)}
     */
    @Deprecated(forRemoval = true)
    public static String getStackedPrefix(ICommandListener self, Guild guild) {
        return self.getStackedPrefix(guild);
    }

    public static MessageOptions attachString(MessageOptions self, String name, String content) {
        return self.addFile(name, content.getBytes(StandardCharsets.UTF_8));
    }

    public static String getLink(Message msg) {
        var g = msg.guild();
        return g == null ? "https://discordapp.com/channels/@me/" + msg.channelIdAsLong() + '/' + msg.idAsLong() :
                "https://discordapp.com/channels/" + g.idAsLong() + '/' + msg.channelIdAsLong() + '/' + msg.idAsLong();
    }

    public static int getColour(Member member) {
        if (member == null) return -1;
        for (var r : member.orderedRoles(Comparator.reverseOrder())) {
            var c = r.color();
            if (c > 0 && c <= 0xFFFFFF) return c;
        }
        return -1;
    }

    public static String avatar(User u) {
        var a = u.avatar();
        return u.effectiveAvatarUrl(new ImageOptions().type(a == null || !a.startsWith("a_") ? ImageType.PNG : ImageType.GIF).size(1024));
    }

    public static boolean canEmbed(Message m) {
        return canEmbed(m.channel());
    }

    public static boolean canEmbed(MessageChannel c) {
        if (!c.isGuild()) return true;
        var gc = c.asGuildChannel();
        return gc.guild().selfMember().hasPermissions(gc, EMBED_LINKS);
    }

    //<editor-fold desc="selfHasPermissions(Message, Permission...)">
    public static boolean selfHasPermissions(Message $, Permission p) {
        return selfHasPermissions($, EnumSet.of(p));
    }

    public static boolean selfHasPermissions(Message $, Permission p1, Permission... p) {
        return selfHasPermissions($, EnumSet.of(p1, p));
    }

    public static boolean selfHasPermissions(Message $, EnumSet<Permission> p) {
        return selfHasPermissions($ == null ? null : $.channel(), p);
    }

    public static boolean selfHasPermissions(MessageChannel $, Permission p) {
        return selfHasPermissions($, EnumSet.of(p));
    }

    public static boolean selfHasPermissions(MessageChannel $, Permission p1, Permission... p) {
        return selfHasPermissions($, EnumSet.of(p1, p));
    }

    public static boolean selfHasPermissions(MessageChannel $, EnumSet<Permission> p) {
        if ($ != null && $.isGuild()) {
            var gc = $.asGuildChannel();
            return gc.guild().selfMember().hasPermissions(gc, p);
        } else {
            return guildRequiredPermissions.containsAll(p);
        }
    }
    //</editor-fold>

    //<editor-fold desc="getSendableChannel(Message, Permission.../Opts)">
    public static Single<? extends MessageChannel> getSendableChannel(Message $) {
        return getSendableChannel($, (MessageOptions) null);
    }

    public static Single<? extends MessageChannel> getSendableChannel(Message $, MessageOptions options) {
        var n = options == null;
        var e = n || options.embed() != null;
        var a = n || options.hasFiles();
        return getSendableChannel($,
                e && a ? EnumSet.of(VIEW_CHANNEL, SEND_MESSAGES, ATTACH_FILES, EMBED_LINKS) :
                        e ? EnumSet.of(VIEW_CHANNEL, SEND_MESSAGES, EMBED_LINKS) :
                                a ? EnumSet.of(VIEW_CHANNEL, SEND_MESSAGES, ATTACH_FILES) :
                                        EnumSet.of(VIEW_CHANNEL, SEND_MESSAGES)
        );
    }

    public static Single<? extends MessageChannel> getSendableChannel(Message $, Permission p) {
        return getSendableChannel($, EnumSet.of(p));
    }

    public static Single<? extends MessageChannel> getSendableChannel(Message $, Permission p1, Permission... p) {
        return getSendableChannel($, EnumSet.of(p1, p));
    }

    public static Single<? extends MessageChannel> getSendableChannel(Message $, EnumSet<Permission> p) {
        return $ == null ? Single.error(new NullPointerException("Message")) : selfHasPermissions($, p) ? Single.just($.channel()) : getDmChannel($.author());
    }
    //</editor-fold>

    //<editor-fold desc="getSendableChannel(MessageChannel, Member, Permission.../Opts)">
    public static Single<? extends MessageChannel> getSendableChannel(MessageChannel $, Member to) {
        return getSendableChannel($, to, (MessageOptions) null);
    }

    public static Single<? extends MessageChannel> getSendableChannel(MessageChannel $, Member to, MessageOptions options) {
        var n = options == null;
        var e = n || options.embed() != null;
        var a = n || options.hasFiles();
        return getSendableChannel($, to,
                e && a ? EnumSet.of(VIEW_CHANNEL, SEND_MESSAGES, ATTACH_FILES, EMBED_LINKS) :
                        e ? EnumSet.of(VIEW_CHANNEL, SEND_MESSAGES, EMBED_LINKS) :
                                a ? EnumSet.of(VIEW_CHANNEL, SEND_MESSAGES, ATTACH_FILES) :
                                        EnumSet.of(VIEW_CHANNEL, SEND_MESSAGES)
        );
    }

    public static Single<? extends MessageChannel> getSendableChannel(MessageChannel $, Member to, Permission p) {
        return getSendableChannel($, to, EnumSet.of(p));
    }

    public static Single<? extends MessageChannel> getSendableChannel(MessageChannel $, Member to, Permission p1, Permission... p) {
        return getSendableChannel($, to, EnumSet.of(p1, p));
    }

    public static Single<? extends MessageChannel> getSendableChannel(MessageChannel $, Member to, EnumSet<Permission> p) {
        return $ == null ? Single.error(new NullPointerException("Message")) : selfHasPermissions($, p) ? Single.just($) : to == null ? Single.error(new NullPointerException("No one to send to.")) : getDmChannel(to);
    }
    //</editor-fold>

    /**
     * Attempts to fetch the output of the {@link Callable callable}, falling back to the supplier if this fails.
     *
     * @param $        The {@link Callable} you wish to fetch the output of.
     * @param supplier The fallback {@link Supplier}.
     */
    public static <T> T getOrDefault(Callable<T> $, Supplier<T> supplier) {
        try {
            return $.call();
        } catch (Exception e) {
            return supplier.get();
        }
    }

    /**
     * Attempts to fetch the output of the {@link Callable callable}, falling back to the supplier if this fails.
     *
     * @param $ The {@link Callable} you wish to fetch the output of.
     * @param t The predefined fallback. Use {@link #getOrDefault(Callable, Supplier)} instead if it's heavy to compute.
     */
    public static <T> T getOrDefault(Callable<T> $, T t) {
        try {
            return $.call();
        } catch (Exception e) {
            return t;
        }
    }

    public static Single<DMChannel> getDmChannel(User u) {
        var c = u.catnip().cache().dmChannels().getById(u.idAsLong());
        return c == null ? u.createDM() : Single.just(c);
    }

    public static Single<DMChannel> getDmChannel(Member u) {
        return getDmChannel(u.user());
    }

    public static Single<DMChannel> getDmChannel(Message u) {
        var c = u.channel();
        return c.isDM() ? Single.just(c.asDMChannel()) : getDmChannel(u.author());
    }

    public static OffsetDateTime now() {
        return OffsetDateTime.now(Clock.systemUTC());
    }
}