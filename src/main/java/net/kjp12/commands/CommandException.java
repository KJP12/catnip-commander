package net.kjp12.commands;

import com.mewna.catnip.entity.message.Embed;
import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.message.MessageOptions;

/**
 * @deprecated Command handlers may make it impossible to handle a command exception; recommenced replacing with a standard message.
 * yes, I know it's deprecated on creation; legacy system compatibility.
 */
@Deprecated(forRemoval = true)
public class CommandException extends RuntimeException {
    private MessageOptions message;

    public CommandException() {
    }

    public CommandException(Embed embed) {
        message = new MessageOptions().embed(embed);
    }

    public CommandException(Message message) {
        super(message.content());
        this.message = new MessageOptions(message);
    }

    public CommandException(String message) {
        super(message);
    }

    public CommandException(String msg, Object... args) {
        super(String.format(msg, args));
    }

    public CommandException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommandException(Throwable cause) {
        super(cause);
    }

    public CommandException(Throwable cause, Message message) {
        this(cause);
        this.message = new MessageOptions(message);
    }

    public final MessageOptions getJDAMessage() {
        return message;
    }
}
