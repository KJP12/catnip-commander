package net.kjp12.commands.abstracts;

import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.message.Message;
import net.kjp12.commands.CategorySystem;
import net.kjp12.commands.utils.MiscellaneousUtils;
import net.kjp12.commands.utils.WebhookClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.UUID;

import static net.kjp12.commands.utils.MiscellaneousUtils.attemptSend;


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
    WebhookClient getWebhook();

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
     * Starts an {@link ICommand} instance via calling {@link ICommand#execute(Message, String)}.
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
        //no more special case handling.
        /*if (cause instanceof CommandException) {
            handleCmdException((CommandException) cause, msg);
            return;
        }*/
        cause.printStackTrace();
        if (msg != null) msg.react("⛔");
        UUID uid = attemptSend(this, cause, msg);
        msg.channel().sendMessage("Something failed; ``" + cause.getMessage() + "``; Error UID: `" + uid + "`");
        LOGGER.error("Error UID: " + uid, cause);
    }

    /**
     * @deprecated Use {@link #handleThrowable(Throwable, Message)} or don't manually throw user-oriented exceptions!
     */
    //signature has been changed to a lower level due to the removal of CommandException.
    @Deprecated(forRemoval = true)
    default void handleCmdException(RuntimeException ce, @Nullable Message msg) {
        handleThrowable(ce, msg); //It is an error to use this.
        /*var hook = getWebhook();
        var cause = ce.getCause();
        var message = ce.getJDAMessage();
        if (cause != null) {
            if (msg != null) msg.react("⛔");
            var uid = attemptSend(this, cause, msg);
            sendMessage(msg, s -> {
                var author = msg.author();
                s.sendMessage(MiscellaneousUtils.genBaseEmbed(0xaa1200, author, msg.guild(), "Something has failed", "UID - " + uid, msg.creationTime())
                        .description("The error has been sent to the owner.")
                        .field("Cause", ce.toString(), false).build()).subscribe(m -> {
                }, e -> handleThrowable(e, null));
                if (message != null) s.sendMessage(message);
            }, t -> {
                attemptSend(this, t, msg);
                hook.send(message);
            });
        } else if (message != null) {
            if (msg == null) hook.send(message);
            else sendMessage(msg, mc -> mc.sendMessage(message), t -> {
                attemptSend(this, t, msg);
                hook.send(message);
            });
        } else {
            var e = MiscellaneousUtils.genBaseEmbed(0xaa1200, msg != null ? msg.author() : null, msg != null ? msg.guild() : null, "Something has failed", null, msg == null ? MiscellaneousUtils.now() : msg.creationTime()).description(ce.getMessage()).build();
            sendMessage(msg, mc -> mc.sendMessage(e), t -> {
                attemptSend(this, t, msg);
                hook.send(e);
            });
        }*/
    }
}
