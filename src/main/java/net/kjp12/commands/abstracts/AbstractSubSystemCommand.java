package net.kjp12.commands.abstracts;//Created on 8/4/18.

import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.message.Message;
import net.kjp12.commands.CategorySystem;
import net.kjp12.commands.CommandException;
import net.kjp12.commands.utils.WebhookClient;

import javax.annotation.Nullable;
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
        if (command != null) startCommand(command, message, p.length > 1 ? p[1].trim() : null);
        else startCommand(defaultCommand, message, ""); //uses default command.
    }

    @Override
    public CategorySystem getCategorySystem() {
        return CMD_CAT;
    }

    @Override
    public ICommand addCommand(String alias, ICommand abstractCommand) {
        return ALIASES.put(alias, abstractCommand);
    }

    @Override
    public ICommand getCommand(String alias) {
        return ALIASES.get(alias);
    }

    @Override
    public void startCommand(ICommand abstractCommand, Message message, String s) {
        abstractCommand.execute(message, s, t -> handleThrowable(t, message));
    }

    @Override
    public WebhookClient getWebhook() {
        return LISTENER.getWebhook();
    }

    @Override
    public String getPrefix(Guild g) {
        return LISTENER.getPrefix(g);
    }

    /**
     * We will delegate to {@link #LISTENER} here.
     */
    @Override
    public void handleThrowable(Throwable throwable, @Nullable Message message) {
        LISTENER.handleThrowable(throwable, message);
    }

    /**
     * We will delegate to {@link #LISTENER} here.
     */
    @Override
    public void handleCmdException(CommandException e, @Nullable Message message) {
        LISTENER.handleCmdException(e, message);
    }
}
