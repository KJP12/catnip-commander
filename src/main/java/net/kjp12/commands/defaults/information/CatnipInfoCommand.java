package net.kjp12.commands.defaults.information;

import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.message.MessageOptions;
import com.mewna.catnip.shard.LifecycleState;
import com.mewna.catnip.shard.manager.ShardManager;
import com.mewna.catnip.util.CatnipMeta;
import net.kjp12.commands.CommandSystemInfo;
import net.kjp12.commands.abstracts.AbstractCommand;
import net.kjp12.commands.abstracts.ICommandListener;
import net.kjp12.commands.utils.GlobalVariables;
import net.kjp12.commands.utils.StringUtils;

public class CatnipInfoCommand extends AbstractCommand {
    private final static LifecycleState[] STATES = LifecycleState.values();

    public CatnipInfoCommand(ICommandListener icl) {
        super(icl);
    }

    @Override
    public void run(Message msg, String args) {
        final var assess = new int[STATES.length];
        var channel = msg.channel();
        var cn = msg.catnip();
        var sm = cn.shardManager();
        var ids = sm.shardIds();
        var s = channel.isGuild() ? (msg.guildIdAsLong() >> 22) % sm.shardCount() : 0;

        var self = cn.selfUser();
        var sb = new StringBuilder("Catnip = ").append(CatnipMeta.VERSION).append("\nCommander = ").append(CommandSystemInfo.VERSION).append('\n').append(StringUtils.stringify(self, false)).append("\n\n");

        for (int id : ids) append(sb, assess, sm, id, id == s);
        sb.append("\n\tAssessment:");

        int total = 0, failed = 0;
        for (int i = 0; i < assess.length; i++)
            if (assess[i] != 0) {
                sb.append("\n\t\t").append(STATES[i]).append(": ").append(assess[i]);
                total += assess[i];
                if (STATES[i] != LifecycleState.CONNECTED && STATES[i] != LifecycleState.LOGGED_IN) failed += assess[i];
            }

        if (sb.insert(0, "Total = " + total + "\nFailing = " + failed + "\nFailure = " + Math.round((double) failed / (double) total * 100) + "%\n").length() < 2000 - 29)
            channel.sendMessage(sb.insert(0, "**__Shard Info__**```diff\n").append("```").toString());
        else
            channel.sendMessage(new MessageOptions().addFile("shardinfo.diff", sb.toString().getBytes(GlobalVariables.charset)));
    }

    void append(StringBuilder $receiver, int[] assess, ShardManager sm, int id, boolean isCurrent) {
        var s = sm.shardState(id).onErrorReturnItem(LifecycleState.DISCONNECTED).blockingGet();
        assess[s.ordinal()]++;
        if (isCurrent) $receiver.append('+');
        else if (s != LifecycleState.CONNECTED && s != LifecycleState.LOGGED_IN) $receiver.append('-');
        $receiver.append("Shard: ").append(id)
                .append("; Status: ").append(s)
                .append("; Heartbeat: ").append(sm.latency(id).onErrorReturnItem(-2L).blockingGet())
                .append('\n');
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
