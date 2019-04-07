package net.kjp12.commands.abstracts;

import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.message.MessageOptions;
import net.kjp12.commands.CategorySystem;
import net.kjp12.commands.CommandException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

public interface ICommand {
    /**
     * Logger for all commands.
     */
    Logger COMMAND_LOGGER = LoggerFactory.getLogger(ICommand.class);

    /**
     * Checks the inherited permissions of all the categories via {@link CategorySystem.Category#checkPermission(Message, ICommand, boolean)}.
     *
     * @param msg Context as {@link Message}. Used for getting Member or User.
     * @param t   If it should throw an exception or return false on failure.
     * @return If the permission check passed.
     * @throws CommandException Permissions check failed and parameter t is false, and is able to throw an exception.
     */
    boolean checkInheritedPermissions(Message msg, boolean t);

    /**
     * Checks the permissions per-command. Maybe used with a permission array like in {@link IUserPermissionCommand#checkRuntimePermission(Message, boolean)}
     *
     * @param msg Context as {@link Message}. Used for getting Member or User.
     * @param t   If it should throw an exception or return false on failure.
     * @return If the permission check passed. Defaults to true.
     * @throws CommandException Permissions check failed and parameter t is false, and is able to throw an exception.
     */
    default boolean checkRuntimePermission(Message msg, boolean t) {
        return true;
    }

    /**
     * Your code for the command instance.
     *
     * @param msg  Context as {@link Message}.
     * @param args Arguments as a singular {@link String}.
     * @throws CommandException When command execution goes wrong and needs to exit quickly. This is a softer runtime exception unless there is another {@link Throwable} wrapped in it.
     * @throws Throwable When anything else goes wrong. Is a harsh exception and will return an error. Will be wrapped in a {@link CommandException} by {@link #execute(Message, String)} unless said method was overridden
     */
    void run(Message msg, String args) throws Throwable;

    /**
     * Method to call when executing a command. It is considered safer to call this as it can properly route the message as needed.
     *
     * @param msg  Context as {@link Message}. Used for permission checks and whatever {@link #run(Message, String)} needs.
     * @param args Arguments as a singular {@link String}.
     * @deprecated Replacing with consumer support to cut down on unneeded lines, with the bonus of added support of stuff.
     */
    @Deprecated
    default void execute(Message msg, String args) {
        execute(msg, args, t -> getListener().handleThrowable(t, msg));
    }

    default void execute(Message msg, String args, @Nonnull Consumer<Throwable> errorHandler) {
        long l = System.nanoTime();
        try {
            COMMAND_LOGGER.debug("[{}] Running.", this);
            if (checkInheritedPermissions(msg, true) && checkRuntimePermission(msg, true))
                run(msg, args == null ? "" : args);
            COMMAND_LOGGER.debug("[{}] Completed within {}ns", this, System.nanoTime() - l);
        } catch (Throwable t) {
            errorHandler.accept(t);
            COMMAND_LOGGER.debug("[{}] Failed in {}ns.", this, System.nanoTime() - l, t); //Having the command system handle it; raising to error will be useless if it will be dumped later.
        }
    }

    /**
     * Makes a message to send to the current channel according to {@link Message#getChannel()}.
     * Should be overridden by {@link AbstractCommand#toMessage(Message)} by default.
     *
     * @param msg Context as {@link Message}. Used for {@link Message#getChannel()} and whatever {@link #toDescription(Message)} needs.
     * @return {@link MessageAction} which maybe used right away with {@link MessageAction#queue()} like in the case of {@link net.kjp12.commands.defaults.information.HelpCommand#run(Message, String)}.
     */
    MessageOptions toMessage(Message msg);

    /**
     * @return {@link String} array of aliases.
     */
    String[] toAliases();

    /**
     * @return {@link String} array of category names.
     */
    String[] toCategories();

    /**
     * @return List of {@link net.kjp12.commands.CategorySystem.Category}
     * */
    List<CategorySystem.Category> getCategoryList();

    /**
     * @return if hidden
     */
    default boolean isHidden() {
        return false;
    }

    /**
     * @return Parent {@link ICommandListener} instance, like {@link AbstractCommand#LISTENER} as overridden at {@link AbstractCommand#getListener()}.
     */
    ICommandListener getListener();

    default String getFirstAliases() {
        var aliases = toAliases();
        return aliases.length <= 0 ? getClass().getSimpleName().toLowerCase() : aliases[0];
    }

    /**
     * Called by {@link #toMessage(Message)} in order to give a short description.
     *
     * @param msg Context as {@link Message}. Maybe used as needed.
     * @return Description as {@link String}
     */
    String toDescription(Message msg);
}
