package net.kjp12.commands.abstracts;

import com.mewna.catnip.entity.builder.EmbedBuilder;
import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.util.Permission;
import net.kjp12.commands.utils.MiscellaneousUtils;

import java.util.ArrayList;
import java.util.EnumSet;

import static com.mewna.catnip.entity.util.Permission.EMBED_LINKS;
import static com.mewna.catnip.entity.util.Permission.SEND_MESSAGES;

/**
 * An interface that allows you to check permissions for the bot-side of commands. Although, most checks can be done internal.
 */
public interface IBotPermissionCommand extends IViewable {
    static String permStr(final Iterable<Permission> set) {
        final var sb = new StringBuilder();
        for (final var p : set) sb.append(p).append('\n');
        return sb.toString();
    }

    /**
     * @return A possibly immutable set of required Runtime {@link Permission}s.
     */
    EnumSet<Permission> requiredRuntimeBotPermissions();

    /**
     * @return A possibly immutable set of required Viewing {@link Permission}s.
     */
    default EnumSet<Permission> requiredViewingBotPermissions() {
        return requiredRuntimeBotPermissions();
    }

    @Override
    default boolean checkRuntimePermission(Message msg, boolean t) {
        return checkBotPermissions(msg, t, requiredRuntimeBotPermissions());
    }

    @Override
    default boolean checkViewingPermission(Message msg, boolean t) {
        return checkBotPermissions(msg, t, requiredViewingBotPermissions());
    }

    default boolean checkBotPermissions(Message msg, boolean t, EnumSet<Permission> arr) {
        var c = msg.channel();
        if (!c.isGuild()) {
            if (MiscellaneousUtils.generalPermissions.containsAll(arr)) return true;
            if (t) c.sendMessage("You must be in a guild to use this command!");
            return false;
        }
        var tc = c.asGuildChannel();
        var m = tc.guild().selfMember();
        if (!m.hasPermissions(tc, arr)) {
            if (t) MiscellaneousUtils.getSendableChannel(msg, SEND_MESSAGES, EMBED_LINKS).subscribe(mc -> {
                var req = new ArrayList<>(arr);
                var eb = new EmbedBuilder().title("I am missing permissions").field("Required", permStr(req), true);
                req.removeAll(m.permissions(tc));
                mc.sendMessage(eb.field("Missing", permStr(req), true).build());
            }, a -> getListener().handleThrowable(a, msg));
            return false;
        }
        return true;
    }
}
