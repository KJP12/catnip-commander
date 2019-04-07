package net.kjp12.commands.abstracts;

import com.mewna.catnip.entity.builder.EmbedBuilder;
import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.util.Permission;
import net.kjp12.commands.CommandException;

import java.util.ArrayList;
import java.util.EnumSet;

/**
 * An interface that allows you to check permissions for the bot-side of commands. Although, most checks can be done internal.
 * */
public interface IBotPermissionCommand extends IViewable {
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
        if (!msg.channel().isGuild()) {
            if (t) {
                throw new CommandException("You must be in a guild to use this command!");
            }
            return false;
        }
        var tc = msg.channel().asGuildChannel();
        var m = tc.guild().selfMember();
        if (!m.hasPermissions(tc, arr)) {
            if (t) {
                var req = new ArrayList<>(arr);
                var eb = new EmbedBuilder().title("I am missing permissions")
                        .field("Required", permStr(req), true);
                req.removeAll(m.permissions(tc));
                throw new CommandException(eb.field("Missing", permStr(req), true).build());
            }
            return false;
        }
        return true;
    }

    static String permStr(final Iterable<Permission> set) {
        final var sb = new StringBuilder();
        for(final var p:set) sb.append(p).append('\n');
        return sb.toString();
    }
}
