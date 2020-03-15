package net.kjp12.commands.defaults.owner;

import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.message.MessageOptions;
import net.kjp12.commands.abstracts.AbstractCommand;
import net.kjp12.commands.abstracts.ICommandListener;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.mewna.catnip.entity.util.Permission.*;
import static net.kjp12.commands.utils.MiscellaneousUtils.*;
import static org.apache.commons.lang3.StringUtils.capitalize;

public class EvaluatorCommand extends AbstractCommand {
    public static final ScriptEngineManager SEM = new ScriptEngineManager();
    private final ScriptEngine engine;

    public EvaluatorCommand(ICommandListener icl, ScriptEngine se) {
        super(icl);
        engine = Objects.requireNonNullElseGet(se, () -> SEM.getEngineByName("nashorn"));
    }

    public EvaluatorCommand(ICommandListener icl, String se) {
        this(icl, SEM.getEngineByName(se));
    }

    public EvaluatorCommand(ICommandListener icl) {
        this(icl, "nashorn");
    }

    @Override
    public void run(Message msg, String args) throws Throwable {
        var author = msg.author();
        var channel = msg.channel();
        var guild = msg.guild();
        var catnip = msg.catnip();
        var shardManager = catnip.shardManager();
        engine.put("api", catnip);
        engine.put("catnip", catnip);
        engine.put("shard", shardManager.shard(guild == null ? 0 : (int) ((guild.idAsLong() >> 22L) % shardManager.shardCount())));
        engine.put("channel", channel);
        engine.put("message", msg);
        engine.put("msg", msg);
        engine.put("server", guild);
        engine.put("guild", guild);
        engine.put("author", author);
        engine.put("commandSystem", LISTENER);
        //insert extension system here
        var o = engine.eval(args);
        engine.put("last", o);
        var s = Objects.toString(o);

        COMMAND_LOGGER.info("Evaluator Input:\n{}\n\nEvaluator Output:\n{}\n", args, s);
        if (s.length() < /*Since there is no variable to reference...*/ 2048 - 11) {
            var e = genBaseEmbed(0x00FF00, 0b10_1000_11111L, author, catnip.selfUser(), "Evaluation", guild, now()).description("```java\n" + s + "```").build();
            getSendableChannel(msg, VIEW_CHANNEL, SEND_MESSAGES, EMBED_LINKS).subscribe(c -> {
                c.sendMessage(e);
                if (c.isDM() && selfHasPermissions(msg, ADD_REACTIONS)) msg.react("📬");
            });
            LISTENER.getWebhook().executeWebhook(e);
        } else {
            var i = new MessageOptions().content("Evaluation Complete! See attached file.").addFile("Eval-" + System.currentTimeMillis() + ".log", s.getBytes(StandardCharsets.UTF_8));
            getSendableChannel(msg, VIEW_CHANNEL, SEND_MESSAGES, ATTACH_FILES).subscribe(c -> {
                c.sendMessage(i);
                if (c.isDM() && selfHasPermissions(msg, ADD_REACTIONS)) msg.react("📬");
            }, e -> LISTENER.handleThrowable(e, msg));
            LISTENER.getWebhook().executeWebhook(i);
        }
    }

    @Override
    public String[] toAliases() {
        return new String[]{"evaluator", "eval"};
    }

    @Override
    public String[] toCategories() {
        return new String[]{"owner only"};
    }

    @Override
    public String toDescription(Message msg) {
        var f = engine.getFactory();
        String lang = capitalize(f.getLanguageName()), eng = capitalize(f.getEngineName()), langv = f.getLanguageVersion(), engv = f.getEngineVersion();
        return "Evaluates stuff using " + (lang.equals(eng) ? capitalize(eng) + ' ' + (langv.equals(engv) ? langv : engv + " (" + langv + ')') : eng + ' ' + engv + " for " + lang + ' ' + langv) + ".\n\nUsage: `" + LISTENER.getPrefix(msg.guild()) + "eval <code>`";
    }
}
