package net.kjp12.commands.defaults.owner;

import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.message.MessageOptions;
import com.mewna.catnip.entity.util.Permission;
import net.kjp12.commands.abstracts.AbstractCommand;
import net.kjp12.commands.abstracts.ICommandListener;
import net.kjp12.commands.utils.MiscellaneousUtils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.capitalize;

public class EvaluatorCommand extends AbstractCommand {
    public static final ScriptEngineManager SEM = new ScriptEngineManager();
    private final ScriptEngine engine;

    public EvaluatorCommand(ICommandListener icl, ScriptEngine se) {
        super(icl);
        hidden = true;
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
        engine.put("api", catnip);
        engine.put("catnip", catnip);
        engine.put("shard", (guild.idAsLong() >> 22L) % (long) catnip.gatewayInfo().shards()); //Would return a shard instance, but there isn't a shard-view implementation, so number will suffice.
        engine.put("channel", channel);
        engine.put("message", msg);
        engine.put("msg", msg);
        engine.put("server", guild);
        engine.put("guild", guild);
        engine.put("author", author);
        engine.put("commandSystem", LISTENER);
        //insert extension system here
        var o = engine.eval(args);
        var s = Objects.toString(o);

        var member = guild == null ? null : guild.selfMember();
        var self = catnip.selfUser();
        if (s.length() < /*Since there is no variable to reference...*/ 2048 - 11 && (member == null || member.hasPermissions(channel.asGuildChannel(), Permission.EMBED_LINKS, Permission.SEND_MESSAGES))) {
            channel.sendMessage(MiscellaneousUtils.genBaseEmbed(0x00FF00, author, guild, "Evaluation", self, MiscellaneousUtils.now()).description("```java\n" + s + "```").build());
        } else if (s.length() < 2000 - 11 && (member != null && member.hasPermissions(channel.asGuildChannel(), Permission.SEND_MESSAGES))) {
            channel.sendMessage("```java\n" + s + "```");
        } else {
            var i = new MessageOptions().content("Evaluation Complete! See attached file.").addFile("Eval-" + System.currentTimeMillis() + ".log", s.getBytes(StandardCharsets.UTF_8));
            if (member != null) catnip.cache().dmChannelAsync(author.idAsLong())
                    .thenAcceptAsync(dm -> dm.sendMessage(i))
                    .exceptionally(e -> {
                        LISTENER.handleThrowable(e, msg);
                        LISTENER.getWebhook().send(i).exceptionally(e2 -> {
                            COMMAND_LOGGER.error("Webhook died.", e2);
                            System.out.println("Evaluator input:\n" + args + "\n\nEvaluator output:\n" + s + "\n");
                            return null;
                        });
                        if (member == null || member.hasPermissions(channel.asGuildChannel(), Permission.ADD_REACTIONS))
                            msg.react("📬");
                        return null;
                    });
            else if (member != null && member.hasPermissions(channel.asGuildChannel(), Permission.SEND_MESSAGES, Permission.ATTACH_FILES)) {
                channel.sendMessage(i);
            } else {
                LISTENER.getWebhook().send(i).exceptionally(e -> {
                    COMMAND_LOGGER.error("Webhook died.", e);
                    System.out.println("Evaluator input:\n" + args + "\n\nEvaluator output:\n" + s + "\n");
                    return null;
                });
                if (member == null || member.hasPermissions(channel.asGuildChannel(), Permission.ADD_REACTIONS))
                    msg.react("📬");
            }
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
