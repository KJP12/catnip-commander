package net.kjp12.commands.defaults.owner;

import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.message.MessageOptions;
import com.mewna.catnip.entity.util.Permission;
import net.kjp12.commands.Executors;
import net.kjp12.commands.abstracts.AbstractCommand;
import net.kjp12.commands.abstracts.AbstractSubSystemCommand;
import net.kjp12.commands.abstracts.ICommandListener;
import net.kjp12.commands.abstracts.IViewable;
import net.kjp12.commands.defaults.information.HelpCommand;
import net.kjp12.commands.utils.MiscellaneousUtils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.kjp12.commands.utils.MiscellaneousUtils.genBaseEmbed;
import static net.kjp12.commands.utils.MiscellaneousUtils.now;
import static net.kjp12.commands.utils.StringUtils.indexOf;

public class ProcessCommand extends AbstractSubSystemCommand {

    public ProcessCommand(ICommandListener cl) {
        super(cl);
        hidden = true;
        defaultCommand = new ProcessList(this);
        new HelpCommand(this);
        new ProcessKill(this);
        new ProcessOutput(this);
        new ProcessExecute(this);
    }

    /**
     * Does not initialize the commands; basically acts like a blank slate.
     *
     * @param cl Command Listener instance to initialize with.
     * @param h  Used to make it hidden or not.
     */
    public ProcessCommand(ICommandListener cl, boolean h) {
        super(cl);
        hidden = h;
    }

    public String[] toAliases() {
        return new String[]{"process"};
    }

    public String[] toCategories() {
        return new String[]{"owner only"};
    }

    public String toDescription(Message msg) {
        return "Process Command";
    }

    public static class ProcessKill extends AbstractCommand implements IViewable {

        public ProcessKill(ICommandListener clb) {
            super(clb);
        }

        @Override
        public void run(Message msg, String arguments) {
            long a;
            try {
                var i = indexOf(arguments, Character::isSpaceChar);
                a = Long.parseUnsignedLong(i < 0 ? arguments : arguments.substring(0, i));
            } catch (NumberFormatException nfe) {
                msg.channel().sendMessage("Not a number.");
                return;
            }
            var pi = Executors.INSTANCE.INSTANCES.get(a);
            if (pi == null) {
                msg.channel().sendMessage("Process not found.");
                return;
            }
            pi.PROCESS.destroy();
            msg.channel().sendMessage("Destroyed " + pi.PID);
        }

        @Override
        public void view(Message msg) {
            //TODO: Interactive. Consider: All processes on system?
            msg.channel().sendMessage("Which process would you like me to kill?");
        }

        @Override
        public String[] toAliases() {
            return new String[]{"kill"};
        }

        @Override
        public String toDescription(Message msg) {
            return "Kills a process.\n\nUsage: `!process kill [PID]`";
        }

        @Override
        public String[] toCategories() {
            return new String[]{"executor"};
        }
    }

    public static class ProcessOutput extends AbstractCommand implements IViewable {

        public ProcessOutput(ICommandListener clb) {
            super(clb);
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        @Override
        public void run(Message msg, String arguments) {
            long a;
            try {
                var i = indexOf(arguments, Character::isSpaceChar);
                a = Long.parseUnsignedLong(i < 0 ? arguments : arguments.substring(0, i));
            } catch (NumberFormatException nfe) {
                msg.channel().sendMessage("Not a number.");
                return;
            }
            var pi = Executors.INSTANCE.INSTANCES.get(a);
            if (pi == null) {
                msg.channel().sendMessage("Process not found.");
                return;
            }
            String out = pi.getOut().toString(), err = pi.getErr().toString();
            boolean $ob = out.isBlank(), $eb = err.isBlank();
            var mc = MiscellaneousUtils.selfHasPermissions(msg, Permission.EMBED_LINKS, Permission.ATTACH_FILES) ? msg.channel() : msg.author().createDM().blockingGet();
            var eb = genBaseEmbed(0x00ff00, msg.author(), msg.guild(), "Process " + pi.PID + " streams.", "", now());
            if ($ob && $eb) mc.sendMessage(eb.description("No process streams available.").build());
            else if ($ob)
                if (err.length() < 2048 - 11)
                    mc.sendMessage(eb.description("```java\n" + err + "```").build());
                else
                    mc.sendMessage(new MessageOptions().embed(eb.build()).addFile("STDERR.txt", err.getBytes(UTF_8)));
            else {
                var opt = out.length() < 2048 - 11 ? new MessageOptions().embed(eb.description("```java\n" + out + "```").build()) :
                        new MessageOptions().embed(eb.build()).addFile("STDOUT.txt", out.getBytes(UTF_8));
                if (!$eb) opt.addFile("STDERR.txt", err.getBytes(UTF_8));
                mc.sendMessage(opt);
            }
        }

        @Override
        public String[] toAliases() {
            return new String[]{"output", "out"};
        }

        @Override
        public String toDescription(Message msg) {
            return "Gets the output from a process.\n\nUsage: `!process output [PID]`";
        }

        @Override
        public String[] toCategories() {
            return new String[]{"executor"};
        }

        @Override
        public void view(Message msg) {
            //TODO: Interactive
            msg.channel().sendMessage("Which process output would you like?");
        }
    }

    public static class ProcessExecute extends AbstractCommand {

        public ProcessExecute(ICommandListener clb) {
            super(clb);
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        @Override
        public void run(Message msg, String arguments) throws Throwable {
            var pie = Executors.INSTANCE.execute(arguments, t -> LISTENER.handleThrowable(t, msg),
                    (Executors.DurianBiConsumer<Executors.ProcessInstance, Integer>) (pi, exit) -> {
                        String out = pi.getOut().toString(), err = pi.getErr().toString();
                        boolean $ob = out.isBlank(), $eb = err.isBlank();
                        var mc = MiscellaneousUtils.selfHasPermissions(msg, Permission.EMBED_LINKS, Permission.ATTACH_FILES) ? msg.channel() : msg.author().createDM().blockingGet();
                        var eb = genBaseEmbed(exit, msg.author(), msg.guild(), "Process " + pi.PID + " exited.", "Process exited with " + exit, now());
                        if ($ob && $eb) mc.sendMessage(eb.description("Process exited. (No Output)").build());
                        else if ($ob)
                            if (err.length() < 2048 - 11)
                                mc.sendMessage(eb.description("```java\n" + err + "```").build());
                            else
                                mc.sendMessage(new MessageOptions().embed(eb.build()).addFile("STDERR.txt", err.getBytes(UTF_8)));
                        else {
                            var opt = out.length() < 2048 - 11 ? new MessageOptions().embed(eb.description("```java\n" + out + "```").build()) :
                                    new MessageOptions().embed(eb.build()).addFile("STDOUT.txt", out.getBytes(UTF_8));
                            if (!$eb) opt.addFile("STDERR.txt", out.getBytes(UTF_8));
                            mc.sendMessage(opt);
                        }
                    });
            pie.getOut().setPushListener(new Executors.DARNewline("STDOUT " + pie.PID));
            pie.getErr().setPushListener(new Executors.DARNewline("STDERR " + pie.PID));
            msg.channel().sendMessage("Running " + pie.PID);
        }

        @Override
        public String[] toAliases() {
            return new String[]{"execute", "exec"};
        }

        @Override
        public String toDescription(Message msg) {
            return "Executes a process.\n\nUsage: `!process execute [Program]`";
        }

        @Override
        public String[] toCategories() {
            return new String[]{"executor"};
        }

    }

    public static class ProcessList extends AbstractCommand {

        public ProcessList(ICommandListener cl) {
            super(cl);
        }

        @Override
        public void run(Message msg, String args) {
            var sb = new StringBuilder("Executors Process Information\n\n");
            for (var i : Executors.INSTANCE.INSTANCES.values()) {
                ProcessHandle.Info info = i.getInfo(); //No risk of failure.
                sb.append(i.PID).append(" -> ").append(info.commandLine().orElse("Unknown?"));
            }
            var mc = msg.channel();
            if (sb.length() < 1990) mc.sendMessage(sb.insert(0, "```css\n").append("```").toString());
            else if (MiscellaneousUtils.selfHasPermissions(msg, Permission.ATTACH_FILES)) {
                mc.sendMessage(MiscellaneousUtils.attachString(new MessageOptions(), "Processes.css", sb.toString()));
            } else
                msg.author().createDM().subscribe(dm -> dm.sendMessage((MiscellaneousUtils.attachString(new MessageOptions(), "Processes.css", sb.toString()))));
        }

        @Override
        public String[] toAliases() {
            return new String[]{"list"};
        }

        @Override
        public String[] toCategories() {
            return new String[]{"executor"};
        }

        @Override
        public String toDescription(Message msg) {
            return "Lists processes if they are still alive.";
        }
    }
}
