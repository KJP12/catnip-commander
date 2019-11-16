package net.kjp12.commands.impl;

//Created on 8/12/18.

import com.mewna.catnip.entity.message.Message;
import net.kjp12.commands.abstracts.AbstractCommand;
import net.kjp12.commands.abstracts.ICommandListener;
import net.kjp12.commands.abstracts.IViewable;

public class IViewImpl extends AbstractCommand implements IViewable {

    public IViewImpl(ICommandListener icl) {
        super(icl);
    }

    public void view(Message message) {
        message.channel().sendMessage(toMessage(message));
    }

    public void run(Message message, String args) {
        message.channel().sendMessage("```" + args.replace("```", "'''") + "```");
    }

    public String[] toAliases() {
        return new String[]{"view", "v"};
    }

    public String[] toCategories() {
        return new String[]{"internal testing"};
    }

    public String toDescription(Message message) {
        return "Command intended for debugging.\n\nUsage: There is none.";
    }
}
