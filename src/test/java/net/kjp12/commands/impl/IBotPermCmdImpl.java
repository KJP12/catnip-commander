package net.kjp12.commands.impl;

//Created on 8/12/18.

import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.util.Permission;
import net.kjp12.commands.abstracts.AbstractCommand;
import net.kjp12.commands.abstracts.IBotPermissionCommand;
import net.kjp12.commands.abstracts.ICommandListener;

import java.util.EnumSet;

public class IBotPermCmdImpl extends AbstractCommand implements IBotPermissionCommand {
    public IBotPermCmdImpl(ICommandListener cl) {
        super(cl);
    }

    public EnumSet<Permission> requiredRuntimeBotPermissions() {
        return EnumSet.of(Permission.MENTION_EVERYONE);
    }

    public void view(Message message) {
        message.channel().sendMessage(toMessage(message));
    }

    public void run(Message message, String args) {
        message.channel().sendMessage(args);
    }

    public String[] toAliases() {
        return new String[]{"botperm", "bp"};
    }

    public String[] toCategories() {
        return new String[]{"internal testing"};
    }

    public String toDescription(Message message) {
        return "Command intended for debugging.\n\nUsage: There is none.";
    }
}
