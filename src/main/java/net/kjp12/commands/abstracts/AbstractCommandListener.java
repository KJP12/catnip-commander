package net.kjp12.commands.abstracts;

import com.mewna.catnip.entity.message.Message;
import net.kjp12.commands.CategorySystem;
import net.kjp12.commands.utils.WebhookClient;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

import static net.kjp12.commands.utils.StringUtils.splitByPredicate;

public abstract class AbstractCommandListener implements ICommandListener {
    public final UUID UNIQUE_LISTENER_ID = new UUID(System.currentTimeMillis(), System.nanoTime());
    public final Map<String, ICommand> ALIASES_MAP = new ConcurrentHashMap<>();
    public final CategorySystem CATEGORY_SYSTEM = new CategorySystem();
    public final ExecutorService POOL;
    protected WebhookClient webhook;

    public AbstractCommandListener(Function<? super AbstractCommandListener, ExecutorService> getPool) {
        POOL = Objects.requireNonNullElseGet(getPool == null ? null : getPool.apply(this), ForkJoinPool::commonPool);
    }

    public AbstractCommandListener(ExecutorService pool) {
        POOL = Objects.requireNonNullElseGet(pool, ForkJoinPool::commonPool);
    }

    public final int hashCode() {
        return UNIQUE_LISTENER_ID.hashCode();
    }

    @Override
    public CategorySystem getCategorySystem() {
        return CATEGORY_SYSTEM;
    }

    @Override
    public WebhookClient getWebhook() {
        return webhook;
    }

    public void setWebhook(WebhookClient wh) {
        webhook = wh;
    }

    @Override
    public ICommand addCommand(String alias, ICommand command) {
        return ALIASES_MAP.put(alias.toLowerCase(), command);
    }

    @Override
    public ICommand getCommand(String alias) {
        return ALIASES_MAP.get(alias.toLowerCase());
    }

    /**
     * Due to restrictions within Catnip that keeps this from being passed as an event listener, being unable to filter out <b>all</b> unwanted events,
     * you should use the following code.
     * <code>
     * AbstractCommandListener acl = $init();
     * Catnip catnip = Catnip.catnipAsync("Token").join();
     * catnip.on(DiscordEvent.MESSAGE_CREATE, acl::onMessageReceived);
     * </code>
     * <p>
     * The code isn't required, you can implement your own variant if you wish.
     *
     * @param m Message object passed from the Catnip Event Bus
     */
    public void onMessageReceived(Message m) {
        if (m.author().bot()) return;
        String raw = m.content(), prefix = getPrefix(m.guild()).toLowerCase();
        if (raw.toLowerCase().startsWith(prefix)) {
            var p = splitByPredicate(raw, Character::isSpaceChar, prefix.length(), 2);
            var cmd = getCommand(p[0]);
            if (cmd != null) startCommand(cmd, m, p.length > 1 ? p[1] : "");
        } else if (raw.length() >= 20) {
            var mention = (raw.charAt(2) == '!' ? "<@!" : "<@") + m.catnip().selfUser().idAsLong() + '>';
            if (raw.startsWith(mention)) {
                var p = splitByPredicate(raw, Character::isSpaceChar, mention.length(), 2);
                if (p.length == 0 || p[0] == null || p[0].isBlank()) {
                    //TODO: Customizable message? *probably could put it in a higher level...*
                    m.channel().sendMessage("**Current prefix is ``" + prefix + "``**\nAlternatively, you can use the mention as a prefix.");
                } else {
                    var cmd = getCommand(p[0]);
                    if (cmd != null) startCommand(cmd, m, p.length > 1 ? p[1] : "");
                }
            }
        }
    }

    @Override
    public void startCommand(ICommand ac, Message msg, String args) {
        POOL.submit(() -> ac.execute(msg, args, t -> handleThrowable(t, msg)));
    }
}
