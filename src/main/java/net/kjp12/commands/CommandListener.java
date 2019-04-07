package net.kjp12.commands;//Created on 12/29/17.

import com.mewna.catnip.entity.guild.Guild;
import net.kjp12.commands.abstracts.AbstractCommandListener;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**This is just a convenience implementation of {@link net.kjp12.commands.abstracts.ICommandListener}.
 * If you need greater control, consider using {@link AbstractCommandListener} or {@link net.kjp12.commands.abstracts.ICommandListener}.*/
public class CommandListener extends AbstractCommandListener implements Thread.UncaughtExceptionHandler {
    public final Function<Guild, String> GET_PREFIX;

    public CommandListener(Function<Guild, String> getPrefix) {
        this(getPrefix, (ExecutorService) null);
    }


    public CommandListener(Function<Guild, String> getPrefix, Function<? super AbstractCommandListener, ExecutorService> commandPool) {
        super(commandPool);
        GET_PREFIX = getPrefix;
        init();
    }
    /**
     * One command listener per shard builder or Catnip inst.
     *
     * @param getPrefix Prefix per guild.
     * @param commandPool Possibly-null Command Pool for use with command execution
     * */
    public CommandListener(Function<Guild, String> getPrefix, ExecutorService commandPool) {
        super(commandPool);
        GET_PREFIX = getPrefix;
        init();
    }

    @Override
    public String getPrefix(Guild guild) {
        return GET_PREFIX.apply(guild);
    }

    private void init() {
        CATEGORY_SYSTEM.buildCategory("owner only", (msg, ac, t) -> false);
        CATEGORY_SYSTEM.buildCategory("guild only", (msg, ac, t) -> {
            if (!msg.channel().isGuild()) {
                if (t)
                    throw new CommandException("Run this in a server!");
                return false;
            }
            return true;
        }).setHidden(true);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        handleThrowable(throwable, null);
    }
}
