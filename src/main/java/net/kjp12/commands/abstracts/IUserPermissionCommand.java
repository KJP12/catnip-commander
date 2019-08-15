package net.kjp12.commands.abstracts;//Created on 1/17/18.

import com.mewna.catnip.entity.builder.EmbedBuilder;
import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.util.Permission;
import net.kjp12.commands.utils.MiscellaneousUtils;

import java.util.ArrayList;
import java.util.EnumSet;

import static com.mewna.catnip.entity.util.Permission.EMBED_LINKS;
import static com.mewna.catnip.entity.util.Permission.SEND_MESSAGES;

public interface IUserPermissionCommand extends IBotPermissionCommand {
    static String permStr(final Iterable<Permission> set) {
        final var sb = new StringBuilder();
        for (final var p : set) sb.append(p).append('\n');
        return sb.toString();
    }

    /**
     * @return A possibly immutable set of required Runtime {@link Permission}s.
     */
    EnumSet<Permission> requiredRuntimePermissions();

    /**
     * @return A possibly immutable set of required Viewing {@link Permission}s.
     */
    default EnumSet<Permission> requiredViewingPermissions() {
        return requiredRuntimePermissions();
    }

    @Override
    default EnumSet<Permission> requiredRuntimeBotPermissions() {
        return requiredRuntimePermissions();
    }

    @Override
    default EnumSet<Permission> requiredViewingBotPermissions() {
        return requiredViewingPermissions();
    }

    @Override
    default boolean checkRuntimePermission(Message msg, boolean t) {
        return checkPermissions(msg, t, requiredRuntimePermissions()) && checkBotPermissions(msg, t, requiredRuntimeBotPermissions());
    }

    @Override
    default boolean checkViewingPermission(Message msg, boolean t) {
        return checkPermissions(msg, t, requiredViewingPermissions()) && checkBotPermissions(msg, t, requiredViewingBotPermissions());
    }

    default boolean checkPermissions(Message msg, boolean t, EnumSet<Permission> arr) {
        var c = msg.channel();
        if (!c.isGuild()) {
            if (MiscellaneousUtils.getGeneralPermissions().containsAll(arr)) return true;
            if (t) c.sendMessage("You must be in a guild to use this command!");
            return false;
        }
        var tc = c.asGuildChannel();
        var m = msg.member();
        if (!m.hasPermissions(tc, arr)) {
            MiscellaneousUtils.getSendableChannel(msg, SEND_MESSAGES, EMBED_LINKS).subscribe(mc -> {
                var req = new ArrayList<>(arr);
                var eb = new EmbedBuilder().title("You're missing permissions").field("Required", permStr(req), true);
                req.removeAll(m.permissions(tc));
                mc.sendMessage(eb.field("Missing", permStr(req), true).build());
            }, a -> getListener().handleThrowable(a, msg));
            return false;
        }
        return true;
    }
}
