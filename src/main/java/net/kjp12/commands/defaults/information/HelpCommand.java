package net.kjp12.commands.defaults.information;

import com.mewna.catnip.entity.channel.MessageChannel;
import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.message.MessageOptions;
import com.mewna.catnip.entity.util.Permission;
import net.kjp12.commands.CategorySystem;
import net.kjp12.commands.abstracts.*;
import net.kjp12.commands.utils.MiscellaneousUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.TreeMap;
import java.util.TreeSet;

import static net.kjp12.commands.utils.StringUtils.splitByPredicate;
import static net.kjp12.commands.utils.StringUtils.stringify;

public class HelpCommand extends AbstractCommand implements IViewable, IBotPermissionCommand {
    private final String description;

    //TODO: Allow dynamic descriptions? Technically done if .help(Message, Channel, String, ICommandListener, ICommand) is used.
    public HelpCommand(ICommandListener icl, String desc) {
        super(icl);
        description = desc;
    }

    public HelpCommand(ICommandListener icl) {
        this(icl, null);
    }

    //TODO: Allow for context sources to be GuildChannel and Member. Both null == default permissions.
    // This will allow for no-message contexts in the future.
    public static MessageOptions help(Message context, MessageChannel channel, String description, ICommandListener listener, @Nullable ICommand self) {
        final var thrown = new ArrayList<Throwable>();
        try {
            var catSys = listener.getCategorySystem();
            var catList = catSys.getCategories();
            var allowed = new ArrayList<CategorySystem.Category>(catList.size() + 1);
            if (self == null) self = listener instanceof ICommand ? (ICommand) listener : listener.getCommand("help");
            allowed.add(catSys.SYSTEM_CATEGORY);
            var catMap = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

            for (var cat : catList)
                try {
                    if (cat.checkPermission(context, self, false)) allowed.add(cat);
                } catch (Throwable thr) {
                    thrown.add(new Exception("Category " + cat.NAME + " threw an error!", thr));
                }

            boolean isEmbed = MiscellaneousUtils.canEmbed(channel);
            StringBuilder sbCmd = null, sbCat = null;
            for (var cat : allowed) {
                if (cat.isHidden()) continue;
                var cmds = cat.getCommands();
                var sortedCmds = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                for (var cmd : cmds) {
                    if (cmd.isHidden() || !allowed.containsAll(cmd.getCategoryList())) continue;

                    try {
                        if (cmd.checkRuntimePermission(context, false))
                            sortedCmds.add(cmd.getFirstAliases());
                    } catch (Throwable t) {
                        sortedCmds.add(cmd.getFirstAliases() + "~");
                        thrown.add(new Exception("Command " + stringify(cmd) + " threw an error!", t));
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
            var guild = channel.isGuild() ? channel.asGuildChannel().guild() : null;
            if (isEmbed) {
                var eb = MiscellaneousUtils.genBaseEmbed(0x46AF2C, channel.catnip(), null, listener instanceof ICommand ? "Help Menu - " + listener.getPrefix(guild) : "Help Menu", guild, null);
                if (description != null) eb.description(description);
                for (var ess : catMap.entrySet()) eb.field(ess.getKey(), ess.getValue(), false);
                return new MessageOptions().embed(eb.build());
            } else {
                var sb = new StringBuilder().append("**__Help Menu");
                if (listener instanceof ICommand)
                    sb.append(" - `").append(listener.getStackedPrefix(guild)).append('`');
                sb.append("__**");
                if (description != null) sb.append(description).append('\n');
                sb.append('\n');
                for (var ess : catMap.entrySet())
                    sb.append(ess.getKey()).append('\n').append(ess.getValue()).append("\n\n");
                return new MessageOptions().parseNoMentions().content(sb.substring(0, sb.length() - 2));
            }
        } finally {
            //This is to ensure that this *always* gets exceptions passed to the listener, even if exceptions get thrown.
            if (!thrown.isEmpty()) {
                var t = new Exception();
                for (var a : thrown) t.addSuppressed(a);
                listener.handleThrowable(t, context);
            }
        }
    }

    @Override
    public void view(Message msg) {
        var toSend = help(msg, msg.channel(), description, LISTENER, this);
        MiscellaneousUtils.getSendableChannel(msg, toSend).subscribe(c -> c.sendMessage(toSend), t -> LISTENER.handleThrowable(t, msg));
    }

    @Override
    public void run(Message msg, String args) {
        var cmd = LISTENER.getCommand(splitByPredicate(args, Character::isSpaceChar, 0, 2)[0]);
        var chan = msg.channel();
        if (cmd != null) chan.sendMessage(cmd.toMessage(msg).parseNoMentions());
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
        var pre = LISTENER.getStackedPrefix(msg.guild());
        return "Returns help to those who need it.\n\n**__Usage__**:\n`" + pre + "help [command]` - Command-specific Help\n`" + pre + "help` - Lists all commands\n\n`Command*` - Bot-side Permission Error\n`Command e` - Permission check threw an error";
    }

    @Override
    public EnumSet<Permission> requiredRuntimeBotPermissions() {
        return EnumSet.of(Permission.SEND_MESSAGES);
    }
}
