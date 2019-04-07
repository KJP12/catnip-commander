package net.kjp12.commands.defaults.owner;

import com.mewna.catnip.entity.message.Message;
import net.kjp12.commands.CommandException;
import net.kjp12.commands.abstracts.AbstractCommand;
import net.kjp12.commands.abstracts.ICommandListener;
import net.kjp12.commands.utils.MiscellaneousUtils;

public class DumpCommand extends AbstractCommand {
    public DumpCommand(ICommandListener icl) {
        super(icl);
    }

    @Override
    public void run(Message msg, String args) throws Throwable {
        var s = args.split(" ", 2);
        var ic = LISTENER.getCommand(s[0]);
        if (ic == null || ic.equals(this))
            throw new CommandException("Cannot dump the command " + (ic == null ? "null pointer" : ic.getFirstAliases()));
        ic.execute(msg, s.length > 1 ? s[1] : "", t -> MiscellaneousUtils.attemptSend(LISTENER, t, msg));
    }

    @Override
    public String[] toAliases() {
        return new String[]{"dump"};
    }

    @Override
    public String[] toCategories() {
        return new String[]{"owner only"};
    }

    @Override
    public String toDescription(Message msg) {
        return "Dumps the inserted command\n\nUsage: `" + MiscellaneousUtils.getStackedPrefix(LISTENER, msg.guild()) + "dump <command> [command-args]`";
    }
}
