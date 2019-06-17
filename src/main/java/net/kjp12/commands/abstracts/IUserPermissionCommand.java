package net.kjp12.commands.abstracts;//Created on 1/17/18.

import com.mewna.catnip.entity.builder.EmbedBuilder;
import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.util.Permission;

import java.util.ArrayList;
import java.util.EnumSet;

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
            if (t) c.sendMessage("You must be in a guild to use this command!");
            return false;
        }
        var tc = c.asGuildChannel();
        var m = msg.member();
        if (!m.hasPermissions(tc, arr)) {
            if (t) {
                var req = new ArrayList<>(arr);
                var eb = new EmbedBuilder().title("You're missing permissions")
                        .field("Required", permStr(req), true);
                req.removeAll(m.permissions(tc));
                c.sendMessage(eb.field("Missing", permStr(req), true).build());
            }
            return false;
        }
        return true;
    }
}
