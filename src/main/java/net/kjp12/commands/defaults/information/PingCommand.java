package net.kjp12.commands.defaults.information;

import com.mewna.catnip.entity.message.Message;
import net.kjp12.commands.abstracts.AbstractCommand;
import net.kjp12.commands.abstracts.ICommandListener;

import java.time.temporal.ChronoUnit;

import static net.kjp12.commands.utils.MiscellaneousUtils.*;

public class PingCommand extends AbstractCommand {
    public PingCommand(ICommandListener icl) {
        super(icl);
    }

    @Override
    public void run(Message msg, String args) {
        var catnip = msg.catnip();
        var sm = catnip.shardManager();
        var guild = msg.guild();
        long b = System.currentTimeMillis();
        getSendableChannel(msg).subscribe(channel -> channel.sendMessage("\uD83C\uDFD3 Please wait...").subscribe(m -> {
            long a = System.currentTimeMillis(), l = a - b;
            var hb = sm.shard(guild == null ? 0 : (int) ((guild.idAsLong() >> 22) % sm.shardCount())).lastHeartbeatLatency();
            if (canEmbed(msg))
                m.edit(genBaseEmbed(0x46AF2C, 0b1000_00000L, null, catnip.selfUser(), "\uD83C\uDFD3 Pong!", msg.guild(), now())
                        .field("⌛ Latency", "**" + l + "ms**", true)
                        .field("⏱ Message Delay", "**" + msg.creationTime().until(m.creationTime(), ChronoUnit.MILLIS) + "ms**", true)
                        .field("\uD83D\uDC93 Heartbeat", "**" + hb + "ms**", true).build());
            else
                m.edit("\uD83C\uDFD3 **__Pong__**!\n⌛ **Latency** = `" + l + "ms`\n⏱ **Message Delay** = `" + (a - msg.creationTime().toInstant().toEpochMilli()) + "ms`\n\uD83D\uDC93 **Heartbeat** = `" + hb + "ms`");
        }, t -> LISTENER.handleThrowable(t, msg)), t -> LISTENER.handleThrowable(t, msg));
    }

    @Override
    public String[] toAliases() {
        return new String[]{"ping"};
    }

    @Override
    public String toDescription(Message msg) {
        return "Pings the current channel.";
    }

    @Override
    public String[] toCategories() {
        return new String[]{"information"};
    }
}
