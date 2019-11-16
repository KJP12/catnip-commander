package net.kjp12.commands.impl;

//Created on 8/12/18.

import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.util.Permission;
import net.kjp12.commands.abstracts.AbstractCommand;
import net.kjp12.commands.abstracts.ICommandListener;
import net.kjp12.commands.abstracts.IUserPermissionCommand;

import java.util.EnumSet;

public class IUserPermCmdImpl extends AbstractCommand implements IUserPermissionCommand {
    public IUserPermCmdImpl(ICommandListener cl) {
        super(cl);
    }

    public EnumSet<Permission> requiredRuntimePermissions() {
        return EnumSet.of(Permission.MENTION_EVERYONE);
    }

    public void view(Message message) {
        message.channel().sendMessage(toMessage(message));
    }

    public void run(Message message, String args) {
        message.channel().sendMessage(args);
    }

    public String[] toAliases() {
        return new String[]{"userperm", "up"};
    }

    public String[] toCategories() {
        return new String[]{"internal testing"};
    }

    public String toDescription(Message message) {
        return "Command intended for debugging.\n\nUsage: There is none.";
    }
}
