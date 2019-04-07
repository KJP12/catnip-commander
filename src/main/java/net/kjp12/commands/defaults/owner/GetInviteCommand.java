package net.kjp12.commands.defaults.owner;//Created on 12/26/17.

import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.guild.Invite;
import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.user.User;
import com.mewna.catnip.rest.invite.InviteCreateOptions;
import net.kjp12.commands.abstracts.AbstractCommand;
import net.kjp12.commands.abstracts.ICommandListener;

import java.util.LinkedList;

import static com.mewna.catnip.entity.util.Permission.CREATE_INSTANT_INVITE;
import static net.kjp12.commands.utils.StringUtils.splitByPredicate;
import static net.kjp12.commands.utils.StringUtils.stringify;

public class GetInviteCommand extends AbstractCommand {
    public GetInviteCommand(ICommandListener clb) {
        super(clb);
        hidden = true;
    }

    @Override
    public void run(Message msg, String arguments) {
        var catnip = msg.catnip();
        var cache = catnip.cache();
        var cat = LISTENER.getCategory().buildCategory("owner only");
        if (!arguments.isBlank() && cat.checkPermission(msg, this, false)) {
            var sb = new StringBuilder("```");
            var guilds = new LinkedList<Guild>();
            for (var a : splitByPredicate(arguments, Character::isSpaceChar, 0, -1)) {
                try {
                    var g = cache.guild(a);
                    if (g != null) guilds.add(g);
                    else guilds.addAll(catnip.cache().guilds().findByName(a, true));
                } catch (NumberFormatException nfe) {
                    guilds.addAll(catnip.cache().guilds().findByName(a, true));
                }
            }
            guilds.addAll(catnip.cache().guilds().findByName(arguments, true));
            guilds.forEach(g -> {
                var i = getInvite(g, msg.author());
                sb.append(stringify(g, true)).append(i == null ? " oh sad" : " https://discord.gg/" + i.code()).append('\n');
            });
            msg.author().createDM().thenAccept(pc -> pc.sendMessage(sb.append("```").toString()).thenAcceptAsync(s -> msg.react("✅")).exceptionally(t -> LISTENER.handleThrowableV(t, msg))).exceptionally(t -> LISTENER.handleThrowableV(t, msg));
        } else {
            if (cat.checkPermission(msg, this, false) || msg.member().hasPermissions(CREATE_INSTANT_INVITE)) {
                var inv = getInvite(msg.guild(), msg.author());
                msg.author().createDM().thenAcceptAsync(pc -> pc.sendMessage(inv == null ? "Couldn't fetch an invite." : "https://discord.gg/" + inv.code()).thenAcceptAsync(s -> msg.react("✅")).exceptionally(t -> LISTENER.handleThrowableV(t, msg))).exceptionally(t -> LISTENER.handleThrowableV(t, msg));
            } else msg.react("❌");
        }
    }

    Invite getInvite(Guild g, User requester) {
        var m = g.selfMember();//, r = g.getMember(requester);
        for (var tc : g.channels()) {
            if (!tc.isCategory() && m.hasPermissions(tc, CREATE_INSTANT_INVITE)) {
                var i = tc.createInvite(new InviteCreateOptions().maxAge(0).maxUses(1).unique(true), "Requested by " + stringify(requester, true)).toCompletableFuture().join();
                if (i != null) return i;
            }
        }
        /*for (var vc : g.getVoiceChannels()) {
            if (m.hasPermission(vc, CREATE_INSTANT_INVITE)) {
                var i = vc.createInvite().setMaxAge(0).setMaxUses(1).reason(stringify("Requested by", requester, true).toString()).complete();
                if (i != null) return i;
            }
        }
        for (var cat : g.getCategories()) {
            if (m.hasPermission(cat, CREATE_INSTANT_INVITE)) {
                var i = new InviteAction(g.getJDA(), cat.getId()).setMaxAge(0).setMaxUses(1).reason(stringify("Requested by", requester, true).toString()).complete();
                if (i != null) return i;
            }
        }
        if (m.hasPermission(MANAGE_CHANNEL, MANAGE_PERMISSIONS)) {
            var tc = (TextChannel) g.getController().createTextChannel("invite-pls")
                    .addPermissionOverride(m, Arrays.asList(CREATE_INSTANT_INVITE, MESSAGE_READ), null)
                    .addPermissionOverride(g.getPublicRole(), null, Collections.singleton(MESSAGE_READ)).complete();
            var i = tc.createInvite().setMaxAge(0).setMaxUses(1).reason(stringify("Requested by", requester, true).toString()).complete();
            if (i != null) return i;
        }*/
        return null;
    }

    @Override
    public String[] toAliases() {
        return new String[]{"getinvite"};
    }

    @Override
    public String toDescription(Message message) {
        return "Gets an invite for the current server.";
    }

    @Override
    public String[] toCategories() {
        return new String[]{"owner only"};
    }
}
