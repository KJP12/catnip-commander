package net.kjp12.commands.defaults.information;

import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.message.MessageOptions;
import com.mewna.catnip.shard.CatnipShard;
import com.mewna.catnip.shard.LifecycleState;
import com.mewna.catnip.util.CatnipMeta;
import net.kjp12.commands.CommandSystemInfo;
import net.kjp12.commands.abstracts.AbstractCommand;
import net.kjp12.commands.abstracts.ICommandListener;
import net.kjp12.commands.utils.MiscellaneousUtils;
import net.kjp12.commands.utils.StringUtils;

import java.nio.charset.StandardCharsets;

public class CatnipInfoCommand extends AbstractCommand {
    private final static LifecycleState[] STATES = LifecycleState.values();

    public CatnipInfoCommand(ICommandListener icl) {
        super(icl);
    }

    @Override
    public void run(Message msg, String args) {
        final var assess = new int[STATES.length];
        var cn = msg.catnip();
        var sm = cn.shardManager();
        var ids = sm.shardIds();
        var s = msg.channel().isGuild() ? (msg.guildIdAsLong() >> 22) % sm.shardCount() : 0;

        var self = cn.selfUser();
        var sb = new StringBuilder("Catnip ").append(CatnipMeta.VERSION).append(" - Commander ").append(CommandSystemInfo.VERSION).append('\n').append(StringUtils.stringify(self, false)).append("\n\n");
        int insert = sb.length() - 1;
        for (int id : ids) {
            CatnipShard sh;
            try {
                sh = sm.shard(id);
            } catch (NullPointerException no) {
                sh = null;
            }
            var lc = sh == null ? LifecycleState.DISCONNECTED : sh.lifecycleState();
            assess[lc.ordinal()]++;
            if (id == s) sb.append('"');
            else if (lc != LifecycleState.CONNECTED && lc != LifecycleState.LOGGED_IN) sb.append('#');
            sb.append("shard: ").append(id)
                    .append("; status: ").append(lc.toString().toLowerCase())
                    .append("; heartbeat: ").append(sh == null ? -2 : sh.lastHeartbeatLatency());
            if (id == s) sb.append('"');
            sb.append('\n');
        }
        sb.append("\n\tAssessment:");

        int total = 0, failed = 0;
        for (int i = 0; i < assess.length; i++)
            if (assess[i] != 0) {
                sb.append("\n\t\t").append(STATES[i]).append(": ").append(assess[i]);
                total += assess[i];
                if (STATES[i] != LifecycleState.CONNECTED && STATES[i] != LifecycleState.LOGGED_IN) failed += assess[i];
            }
        sb.insert(insert, "Failure Rate: " + Math.round((double) failed / (double) total * 100) + "% [" + failed + " / " + total + "]\n");
        MiscellaneousUtils.getSendableChannel(msg).subscribe(c -> {
            if (sb.length() < 2000 - 29)
                c.sendMessage(sb.insert(0, "**__Shard Info__**```c\n").append("```").toString());
            else
                c.sendMessage(new MessageOptions().addFile("shardinfo.txt", sb.toString().getBytes(StandardCharsets.UTF_8)));
        }, t -> LISTENER.handleThrowable(t, msg));
    }

    @Override
    public String[] toAliases() {
        return new String[]{"shardinfo"};
    }

    @Override
    public String toDescription(Message msg) {
        return "Gets the information of every shard attached.";
    }

    @Override
    public String[] toCategories() {
        return new String[]{"information"};
    }
}
