package net.kjp12.commands.abstracts;

import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.message.MessageOptions;
import com.mewna.catnip.entity.util.Permission;
import net.kjp12.commands.CategorySystem;
import net.kjp12.commands.utils.MiscellaneousUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.kjp12.commands.utils.StringUtils.stringify;

public abstract class AbstractCommand implements ICommand {
    public final ICommandListener LISTENER;
    public boolean hidden = false;
    private final List<CategorySystem.Category> CATEGORIES, UNMODIFIABLE;// = new ArrayList<>();

    public AbstractCommand(ICommandListener icl){
        LISTENER = icl;
        var aliases = toAliases();
        if(aliases == null || aliases.length <= 0) {
            aliases = new String[]{getFirstAliases()};
            COMMAND_LOGGER.warn("[{}] Missing aliases, will be loaded with {}.", stringify(this), aliases[0]);
            var cmd = icl.addCommand(aliases[0], this);
            if(cmd != null) COMMAND_LOGGER.error("[{}] As {}, clashed with {}!", stringify(this), aliases[0], stringify(cmd));
        } else for (var a:aliases){
            var cmd = icl.addCommand(a, this);
            if(cmd != null) COMMAND_LOGGER.error("[{}] As {}, clashed with {}!", stringify(this), a, stringify(cmd));
        }
        var isDep = getClass().isAnnotationPresent(Deprecated.class);
        var catSys = icl.getCategorySystem();
        var cats = toCategories();
        UNMODIFIABLE = Collections.unmodifiableList(CATEGORIES = new ArrayList<>(cats.length + (isDep ? 2 : 1)));
        for(var cat : toCategories()) CATEGORIES.add(catSys.buildCategory(cat).addCommand(this));
        CATEGORIES.add(catSys.SYSTEM_CATEGORY.addCommand(this));
        if(isDep) {
            COMMAND_LOGGER.warn("[{}] Deprecated! Assigning deprecated...", stringify(this));
            CATEGORIES.add(catSys.DEPRECATED_CATEGORY);
        }
    }

    public final boolean checkInheritedPermissions(Message msg, boolean t){
        for(var cat:CATEGORIES) if(!cat.checkPermission(msg, this, t)) return false;
        return true;
    }

    public MessageOptions toMessage(Message msg){
        if(hidden && !(checkInheritedPermissions(msg, true) && checkRuntimePermission(msg, true)))
            return new MessageOptions().content("Help for a hidden command disallowed.");
        var aliases = toAliases();
        var channel = msg.channel();
        if(!channel.isGuild() || msg.guild().selfMember().hasPermissions(channel.asGuildChannel(), Permission.EMBED_LINKS)) {
            var eb = MiscellaneousUtils.genBaseEmbed(0x46AF2C, msg.author(), msg.guild(), getFirstAliases() + " | " + getClass().getSimpleName(), null, msg.creationTime()).description(toDescription(msg));
            if(aliases != null && aliases.length > 0) {
                var sb = new StringBuilder();
                for (var a : aliases) sb.append('`').append(a).append("`, ");
                eb.field("Aliases", sb.substring(0, sb.length() - 2), false);
            }
            var sb = new StringBuilder();
            for(var c : CATEGORIES) if(!c.isHidden()) sb.append('`').append(StringUtils.capitalize(c.NAME)).append("`, ");
            eb.field("Categories", sb.substring(0, sb.length() - 2), false);
            return new MessageOptions().embed(eb.build());
        } else {
            var sb = new StringBuilder("**__").append(getFirstAliases()).append(" | ").append(getClass().getSimpleName()).append("__**\n\n").append(toDescription(msg));
            if(aliases != null && aliases.length > 0) {
                sb.append("\n\n__Aliases__\n");
                for (var a : aliases) sb.append('`').append(a).append("`, ");
                int len = sb.length();
                sb.delete(len - 2, len);
            }
            sb.append("\n\n__Categories__\n");
            for(var c : CATEGORIES) if(!c.isHidden()) sb.append('`').append(c.NAME).append("`, ");
            return new MessageOptions().content(sb.substring(0, sb.length() - 2));
        }
    }

    public final boolean isHidden() {
        return hidden;
    }

    public final ICommandListener getListener(){
        return LISTENER;
    }

    public final String getFirstAliases() {
        return ICommand.super.getFirstAliases();
    }

    public final String toString() {
        return getFirstAliases() + " | " + getClass().getSimpleName();
    }

    public final List<CategorySystem.Category> getCategoryList(){
        return UNMODIFIABLE; //Help optimization.
    }

    public final int hashCode() {
        return getClass().hashCode();
    }

    public final boolean equals(Object obj){
        return obj == this || (obj != null && obj.getClass().equals(getClass()));
    }
}
