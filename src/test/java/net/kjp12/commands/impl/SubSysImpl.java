package net.kjp12.commands.impl;

import com.mewna.catnip.entity.message.Message;
import net.kjp12.commands.abstracts.AbstractSubSystemCommand;
import net.kjp12.commands.abstracts.ICommandListener;

public class SubSysImpl extends AbstractSubSystemCommand {

    public SubSysImpl(ICommandListener cl) {
        super(cl);
    }

    public String[] toAliases() {
        return new String[]{"subsys", "ss"};
    }

    public String[] toCategories() {
        return new String[]{"internal testing"};
    }

    public String toDescription(Message message) {
        return "Command intended for debugging.\n\nUsage: There is none.";
    }
}