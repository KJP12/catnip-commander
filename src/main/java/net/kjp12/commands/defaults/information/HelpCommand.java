package net.kjp12.commands.defaults.information;

import com.mewna.catnip.entity.message.Message;
import io.reactivex.functions.BiConsumer;
import net.kjp12.commands.CategorySystem;
import net.kjp12.commands.abstracts.AbstractCommand;
import net.kjp12.commands.abstracts.ICommandListener;
import net.kjp12.commands.abstracts.IViewable;
import net.kjp12.commands.utils.MiscellaneousUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

import static net.kjp12.commands.utils.StringUtils.splitByPredicate;
import static net.kjp12.commands.utils.StringUtils.stringify;

public class HelpCommand extends AbstractCommand implements IViewable {
    private final String description;

    public HelpCommand(ICommandListener icl, String desc) {
        super(icl);
        description = desc;
    }

    public HelpCommand(ICommandListener icl) {
        this(icl, null);
    }

    @Override
    public void view(Message msg) {
        var channel = msg.channel();
        channel.sendMessage("Generating Help...").subscribe((BiConsumer<? super Message, ? super Throwable>) (m, t) -> {
            var catSys = LISTENER.getCategorySystem();
            var catList = catSys.getCategories();
            var allowed = new ArrayList<CategorySystem.Category>(catList.size());
            if (catSys.SYSTEM_CATEGORY.checkPermission(msg, this, false)) allowed.add(catSys.SYSTEM_CATEGORY);
            else m.edit("Command system is locked down. How did you execute this?");
            var catMap = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

            for (var cat : catList)
                try {
                    if (cat.checkPermission(msg, this, false)) allowed.add(cat);
                } catch (Throwable thr) {
                    //TODO: LISTENER#handleThrowable(String reason, Throwable cause, Message msg)
                    LISTENER.handleThrowable(new Exception("Category " + cat.NAME + " threw an error!", thr), msg);
                }

            boolean isEmbed = MiscellaneousUtils.canEmbed(msg);
            StringBuilder sbCmd = null, sbCat = null;
            for (var cat : allowed) {
                if (cat.isHidden()) continue;
                var cmds = cat.getCommands();
                var sortedCmds = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                boolean noError = cat.checkPermission(m, this, false);
                cmd:
                for (var cmd : cmds) {
                    if (cmd.isHidden() || !allowed.containsAll(cmd.getCategoryList())) continue;

                    try {
                        if (cmd.checkRuntimePermission(msg, false))
                            sortedCmds.add((noError && cmd.checkRuntimePermission(m, false)) ? cmd.getFirstAliases() : cmd.getFirstAliases() + "*");
                    } catch (Throwable cmdt) {
                        sortedCmds.add(cmd.getFirstAliases() + " e");
                        //TODO: LISTENER#handleThrowable(String reason, Throwable cause, Message msg)
                        LISTENER.handleThrowable(new Exception("Command " + stringify(cmd) + " threw an error!", t), msg);
                    }
                }

                if (!sortedCmds.isEmpty()) {
                    if (sbCmd == null) sbCmd = new StringBuilder(sortedCmds.size() * 18);
                    else sbCmd.setLength(0);
                    if (sbCat == null) {
                        sbCat = new StringBuilder(cat.NAME.length() + (isEmbed ? 1 : 4));
                        if (!isEmbed) sbCat.append("**");
                    } else sbCat.setLength(isEmbed ? 0 : 2);
                    for (var cmd : sortedCmds) sbCmd.append('`').append(cmd).append("`, ");
                    sbCat.append(StringUtils.capitalize(cat.NAME));
                    if (!isEmbed) sbCat.append("**");
                    catMap.put(sbCat.toString(), sbCmd.substring(0, sbCmd.length() - 2));
                }
            }
            if (isEmbed) {
                var eb = MiscellaneousUtils.genBaseEmbed(0x46AF2C, msg.author(), msg.guild(), "Help Menu", null, msg.creationTime());
                if (description != null) eb.description(description);
                for (var ess : catMap.entrySet()) eb.field(ess.getKey(), ess.getValue(), false);
                m.edit(eb.build());
            } else {
                var sb = new StringBuilder("**__Help Menu__**\n");
                if (description != null) sb.append(description).append('\n');
                sb.append('\n');
                for (var ess : catMap.entrySet())
                    sb.append(ess.getKey()).append('\n').append(ess.getValue()).append("\n\n");
                m.edit(sb.substring(0, sb.length() - 2));
            }
        });
    }

    @Override
    public void run(Message msg, String args) throws Throwable {
        var cmd = LISTENER.getCommand(splitByPredicate(args, Character::isSpaceChar, 0, 2)[0]);
        var chan = msg.channel();
        if (cmd != null) chan.sendMessage(cmd.toMessage(msg));
        else chan.sendMessage("Command wasn't found.");
    }

    @Override
    public String[] toAliases() {
        return new String[]{"help", "h", "?"};
    }

    @Override
    public String[] toCategories() {
        return new String[]{"information"};
    }

    @Override
    public String toDescription(Message msg) {
        var pre = MiscellaneousUtils.getStackedPrefix(LISTENER, msg.guild());
        return "Returns help to those who need it.\n\n**__Usage__**:\n`" + pre + "help [command]` - Command-specific Help\n`" + pre + "help` - Lists all commands\n\n`Command*` - Bot-side Permission Error\n`Command e` - Permission check thrown an error";
    }
}
