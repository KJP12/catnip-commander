package net.kjp12.commands.defaults.information;

import com.mewna.catnip.entity.message.Message;
import net.kjp12.commands.abstracts.AbstractCommand;
import net.kjp12.commands.abstracts.ICommandListener;
import net.kjp12.commands.utils.MiscellaneousUtils;

public class PingCommand extends AbstractCommand {
    public PingCommand(ICommandListener icl) {
        super(icl);
    }

    @Override
    public void run(Message msg, String args) {
        var catnip = msg.catnip();
        var channel = msg.channel();
        var sm = catnip.shardManager();
        var guild = msg.guild();
        var s = guild == null ? 0 : (guild.idAsLong() >> 22) % sm.shardCount();
        long b = System.currentTimeMillis();
        channel.triggerTypingIndicator().subscribe(() -> {
            long a = System.currentTimeMillis(), l = a - b;
            sm.latency((int) s).subscribe(hb -> {
                if (MiscellaneousUtils.canEmbed(msg))
                    channel.sendMessage(MiscellaneousUtils.genBaseEmbed(0x46AF2C, null, msg.guild(), "\uD83C\uDFD3 Pong!", null, MiscellaneousUtils.now())
                            .field("⌛ Latency", "**" + l + "ms**", true)
                            .field("⏱ Message Delay", "**" + (a - msg.creationTime().toInstant().toEpochMilli()) + "ms**", true)
                            .field("\uD83D\uDC93 Heartbeat", "**" + hb + "ms**", true).build());
                else
                    channel.sendMessage("\uD83C\uDFD3 **__Pong__**!\n⌛ **Latency** = `" + l + "ms`\n⏱ **Message Delay** = `" + (a - msg.creationTime().toInstant().toEpochMilli()) + "ms`\n\uD83D\uDC93 **Heartbeat** = `" + hb + "ms`");
            }, t -> LISTENER.handleThrowable(t, msg));
        }, t -> LISTENER.handleThrowable(t, msg));
        COMMAND_LOGGER.info("Ping..?");
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
