package net.kjp12.commands.abstracts;

import com.mewna.catnip.entity.channel.Webhook;
import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.util.Permission;
import net.kjp12.commands.CategorySystem;
import net.kjp12.commands.utils.MiscellaneousUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.function.Consumer;

import static net.kjp12.commands.utils.MiscellaneousUtils.*;


public interface ICommandListener {
    /**
     * Logger for all command listeners.
     */
    Logger LOGGER = LoggerFactory.getLogger(ICommandListener.class);

    /**
     * @return {@link CategorySystem} for use with new instances of {@link ICommand}, like {@link AbstractCommand}.
     */
    CategorySystem getCategorySystem();

    /**
     * @param guild Guild for the prefix
     * @return Prefix as String
     */
    String getPrefix(Guild guild);

    /**
     * @return WebhookClient within the Command Listener.
     */
    Webhook getWebhook();

    /**
     * Adds a command to this command system.
     *
     * @param alias   An alias for the {@link ICommand} instance.
     * @param command The command instance to bind.
     * @return A {@link ICommand} if there was already a command with that aliases. Will log warnings in {@link AbstractCommand}'s constructor if this happens.
     */
    ICommand addCommand(String alias, ICommand command);

    /**
     * Gets a command via its alias.
     *
     * @param alias An alias to get the {@link ICommand} instance by.
     * @return A {@link ICommand} if it exists or was defaulted to.
     */
    ICommand getCommand(String alias);

    /**
     * Starts an {@link ICommand} instance via calling {@link ICommand#execute(Message, String, Consumer)}.
     * The specific implementation may execute on the spot or dump to an {@link java.util.concurrent.ExecutorService}.
     *
     * @param ac   The command to execute.
     * @param msg  Context as {@link Message}.
     * @param args Arguments as a singular {@link String}.
     */
    default void startCommand(ICommand ac, Message msg, String args) {
        //providing default method as we can just start it here.
        ac.execute(msg, args, t -> handleThrowable(t, msg));
    }

    /**
     * Will always call {@link MiscellaneousUtils#attemptSend(ICommandListener, Throwable, Message)}.
     *
     * @param cause The error.
     * @param msg   Context as {@link Message}. Gives all the information like the raw message, who ran it, etc.
     */
    default void handleThrowable(Throwable cause, @Nullable Message msg) {
        var uid = attemptSend(this, cause, msg);
        if (msg != null) {
            if (selfHasPermissions(msg, Permission.ADD_REACTIONS)) msg.react("⛔");
            getSendableChannel(msg).subscribe(c -> c.sendMessage("⛔ ``Error " + uid + ": " + cause.getClass() + ": " + cause.getMessage() + "``"));
        }
        LOGGER.error("Error UID: " + uid, cause);
    }
}
