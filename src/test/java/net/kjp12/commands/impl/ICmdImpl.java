package net.kjp12.commands.impl;

//Created on 8/12/18.

import com.mewna.catnip.entity.message.Message;
import net.kjp12.commands.abstracts.AbstractCommand;
import net.kjp12.commands.abstracts.ICommandListener;

public class ICmdImpl extends AbstractCommand {
    public ICmdImpl(ICommandListener cl) {
        super(cl);
    }

    public void run(Message message, String args) {
        message.channel().sendMessage("```" + args.replace("```", "'''") + "```");
    }

    public String[] toAliases() {
        return new String[]{"cmd", "c"};
    }

    public String[] toCategories() {
        return new String[]{"internal testing"};
    }

    public String toDescription(Message message) {
        return "Command intended for debugging.\n\nUsage: There is none.";
    }
}
