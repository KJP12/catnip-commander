import com.mewna.catnip.Catnip;
import com.mewna.catnip.CatnipOptions;
import com.mewna.catnip.entity.user.Presence;
import com.mewna.catnip.entity.user.User;
import com.mewna.catnip.rest.ratelimit.DefaultRateLimiter;
import com.mewna.catnip.rest.requester.SerialRequester;
import com.mewna.catnip.shard.DiscordEvent;
import com.mewna.catnip.shard.manager.DefaultShardManager;
import net.kjp12.commands.CommandListener;
import net.kjp12.commands.defaults.information.CatnipInfoCommand;
import net.kjp12.commands.defaults.information.HelpCommand;
import net.kjp12.commands.defaults.information.PingCommand;
import net.kjp12.commands.defaults.owner.DumpCommand;
import net.kjp12.commands.defaults.owner.EvaluatorCommand;
import net.kjp12.commands.defaults.owner.GetInviteCommand;
import net.kjp12.commands.defaults.owner.ProcessCommand;
import net.kjp12.commands.utils.WebhookClient;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.util.stream.IntStream;

import static net.kjp12.commands.utils.MiscellaneousUtils.getOrDefault;

public class MainTest {
    public static User owner = null;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("You are missing several arguments!");
            System.out.println("Please provide <token> [node] [nodes] [shards] [proxy-hostname] [proxy-port] [webhook-id] [webhook-token]");
            System.out.println("(token is required, sharding is optional)");
            System.exit(1);
        }
        var options = new CatnipOptions(args[0]);
        if (args.length >= 6 && !args[4].equals("null") && !args[5].equals("null")) {
            options.requester(new SerialRequester(new DefaultRateLimiter(), HttpClient.newBuilder().proxy(ProxySelector.of(new InetSocketAddress(args[4], Integer.parseInt(args[5]))))));
        }
        int node = args.length < 4 ? 0 : getOrDefault(() -> Integer.parseInt(args[1]), 0),
                nodes = args.length < 4 ? 1 : getOrDefault(() -> Integer.parseInt(args[2]), 1),
                shards = args.length < 4 ? 1 : getOrDefault(() -> Integer.parseInt(args[3]), 1),
                tmp = (shards + nodes - 1) / nodes,
                offset = tmp * node;
        options.shardManager(new DefaultShardManager(IntStream.range(offset, offset + (offset + tmp > shards ? shards - offset : tmp))));
        var catnip = Catnip.catnip(options);
        var cl = new CommandListener(g -> "cl!");
        cl.CATEGORY_SYSTEM.buildCategory("owner only", (msg, ic, t) -> msg.author().equals(owner));
        if (args.length >= 8) {
            cl.setWebhook(new WebhookClient(catnip, args[6], args[7]));
        } else
            cl.setWebhook(new WebhookClient(catnip, "503150567527284736", "6qIOWd3frKSkcjI9brPTBUutG4KIhaNPFhzD9s1BvYbhLWCzYZEdIFILhMT3lxigGiJC")) /*effectively no-op*/;
        new DumpCommand(cl);
        new HelpCommand(cl);
        new CatnipInfoCommand(cl);
        new PingCommand(cl);
        new GetInviteCommand(cl);
        new ProcessCommand(cl);
        new EvaluatorCommand(cl, EvaluatorCommand.SEM.getEngineByName("groovy"));
        catnip.presence(Presence.OnlineStatus.DND, "Command System Debugging", Presence.ActivityType.PLAYING, null);
        catnip.observable(DiscordEvent.MESSAGE_CREATE).forEach(cl::onMessageReceived);
        catnip.rest().user().getCurrentApplicationInformation().subscribe(ai -> owner = ai.owner());
        catnip.connect();
    }
}
