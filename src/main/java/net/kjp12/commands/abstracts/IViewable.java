package net.kjp12.commands.abstracts;

import com.mewna.catnip.entity.message.Message;
import net.kjp12.commands.CommandException;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

import static net.kjp12.commands.utils.StringUtils.stringify;

public interface IViewable extends ICommand {
    /**
     * Your code for the command instance.
     *
     * @param msg Context as {@link Message}
     * @throws CommandException When command execution goes wrong and needs to exit quickly. This is a softer runtime exception unless there is another {@link Throwable} wrapped in it.
     * @throws Throwable        When anything else goes wrong. Is a harsh exception and will return an error. Will be wrapped in a {@link CommandException} by {@link #execute(Message, String)} unless said method was overridden
     */
    void view(Message msg) throws Throwable;

    /**
     * Method to call when executing a command. It is considered safer to call this as it can properly route the message as needed.
     * This method also checks to see if args is empty or null so it can see if it should run {@link #view(Message)} or {@link #run(Message, String)}.
     *
     * @param msg  Context as {@link Message}. Used for permission checks and whatever {@link #run(Message, String)} or {@link #view(Message)} needs.
     * @param args Arguments as a singular {@link String}.
     * @param errorHandler Handles the errors that get thrown within the command.
     */
    @Override
    default void execute(Message msg, String args, @Nonnull Consumer<Throwable> errorHandler) {
        long l = System.nanoTime();
        try {
            COMMAND_LOGGER.debug('[' + stringify(this) + "] Running...");
            if (checkInheritedPermissions(msg, true))
                if (args != null && !args.isBlank()) {
                    if (checkRuntimePermission(msg, true)) run(msg, args);
                } else if (checkViewingPermission(msg, true)) view(msg);
            COMMAND_LOGGER.debug('[' + stringify(this) + "] Completed within " + (System.nanoTime() - l) + "ns");
        } catch (Throwable t) {
            errorHandler.accept(t);
            COMMAND_LOGGER.debug('[' + stringify(this) + "] Failed in " + (System.nanoTime() - l) + "ns. Current stack.", new Throwable());
        }
    }

    /**
     * Checks the permissions per-command. Maybe used with a permission array like in {@link IUserPermissionCommand#checkViewingPermission(Message, boolean)}
     *
     * @param msg Context as {@link Message}. Used for getting Member or User.
     * @param t   If it should throw an exception or return false on failure.
     * @return If the permission check passed. Defaults to using {@link #checkRuntimePermission(Message, boolean)}.
     * @throws CommandException Permissions check failed and parameter t is false, and is able to throw an exception.
     */
    default boolean checkViewingPermission(Message msg, boolean t) {
        return checkRuntimePermission(msg, t);
    }
}
