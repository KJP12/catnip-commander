package net.kjp12.commands.abstracts;//Created on 8/4/18.

import com.mewna.catnip.entity.channel.Webhook;
import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.message.Message;
import net.kjp12.commands.CategorySystem;
import net.kjp12.commands.defaults.information.HelpCommand;
import net.kjp12.commands.utils.MiscellaneousUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractSubSystemCommand extends AbstractCommand implements ICommandListener {
    protected final Map<String, ICommand> ALIASES = new ConcurrentHashMap<>();
    protected final CategorySystem CMD_CAT;
    /**
     * Default command for use when no other commands matched.
     * <p>
     * Please initialize on object initialization if used.
     */
    protected ICommand defaultCommand;

    public AbstractSubSystemCommand(ICommandListener cl) {
        super(cl);
        //Inherit from our super's system category.
        CMD_CAT = new CategorySystem(cl.getCategorySystem());
    }

    /**
     * Because we are an {@link ICommandListener} and not a standard command, we need to act like a command listener here.
     * <p>
     * We will {@link String#split(String, int)} with the regex "[\\h\\s\\v\\n\\r]" into two arrays.
     * Then {@link #getCommand(String)} and if nonnull, execute it.
     */
    @Override
    public void run(Message message, String args) {
        var p = args.split("[\\h\\s\\v\\n\\r]", 2);
        var command = getCommand(p[0].trim());
        if (command != null) startCommand(command, message, p.length > 1 ? p[1].trim() : "");
        else if (defaultCommand != null) startCommand(defaultCommand, message, ""); //uses default command.
        else {
            var msg = HelpCommand.help(message, message.channel(), "", this, this);
            MiscellaneousUtils.getSendableChannel(message, msg).subscribe(c -> c.sendMessage(msg));
        }
    }

    @Override
    public CategorySystem getCategorySystem() {
        return CMD_CAT;
    }

    public ICommand setDefaultCommand(ICommand ic) {
        if (ic.getListener() != this)
            throw new IllegalArgumentException("Command " + ic + " does not belong to " + this);
        var old = defaultCommand;
        defaultCommand = ic;
        return old;
    }

    @Override
    public ICommand addCommand(String alias, ICommand abstractCommand) {
        return ALIASES.put(alias.toLowerCase(), abstractCommand);
    }

    @Override
    public ICommand getCommand(String alias) {
        return ALIASES.get(alias.toLowerCase());
    }

    @Override
    @ParametersAreNonnullByDefault
    public void startCommand(ICommand abstractCommand, Message message, String s) {
        abstractCommand.execute(message, s, t -> handleThrowable(t, message));
    }

    @Override
    public Webhook getWebhook() {
        return LISTENER.getWebhook();
    }

    @Override
    public String getPrefix(Guild g) {
        return LISTENER.getPrefix(g) + getFirstAliases() + ' ';
    }

    @Override
    public void handleThrowable(Throwable throwable, @Nullable Message message) {
        LISTENER.handleThrowable(throwable, message);
    }
}
