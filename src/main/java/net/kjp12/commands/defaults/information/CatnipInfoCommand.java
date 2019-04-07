package net.kjp12.commands.defaults.information;

import com.mewna.catnip.entity.message.Message;
import net.kjp12.commands.abstracts.AbstractCommand;
import net.kjp12.commands.abstracts.ICommandListener;
import net.kjp12.commands.utils.MiscellaneousUtils;

public class CatnipInfoCommand extends AbstractCommand {
    public CatnipInfoCommand(ICommandListener icl) {
        super(icl);
    }

    @Override
    public void run(Message msg, String args){
        var channel = msg.channel();
        var sm = msg.catnip().shardManager();
        var ids = sm.shardIds();
        var failing = new java.util.HashSet<Integer>(ids.size());

        for(int id:ids) if(sm.isConnected(id).toCompletableFuture().join() != true) failing.add(id);
        int total = ids.size(), failed = failing.size();
        if(MiscellaneousUtils.canEmbed(msg)){
            var eb = MiscellaneousUtils.genBaseEmbed(0x46AF2C, msg.author(), msg.guild(), "Catnip Info", null, msg.creationTime());
            eb.description("Total = " + total + " | Failing = " + failed + "\nThat's " + Math.round((double) failed / (double) total * 100) + "% failure!");
            if(!failing.isEmpty()) eb.field("Failing Shards", failing.toString(), false);
            channel.sendMessage(eb.build());
        } else {
            var str = "**__Catnip Info__**\nTotal = " + total + " | Failing = " + failed + "\nThat's " + Math.round((double) failed / (double) total * 100) + "% failure!";
            if(!failing.isEmpty()) str += "\n\n**__Failing Shards__**\n```json\n" + failing.toString() + "```";
            channel.sendMessage(str);
        }
    }

    @Override public String[] toAliases() {
        return new String[]{"shardinfo"};
    }

    @Override
    public String toDescription(Message msg){
        return "Gets the information of every shard attached.";
    }

    @Override
    public String[] toCategories(){
        return new String[]{"information"};
    }
}
