package net.kjp12.commands.defaults.owner;

import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.message.MessageOptions;
import net.kjp12.commands.Executors;
import net.kjp12.commands.abstracts.*;
import net.kjp12.commands.defaults.information.HelpCommand;

import static com.mewna.catnip.entity.util.Permission.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.kjp12.commands.utils.MiscellaneousUtils.*;
import static net.kjp12.commands.utils.StringUtils.indexOf;

public class ProcessCommand extends AbstractSubSystemCommand {
    private ICommand help = null;
    public ProcessCommand(ICommandListener cl) {
        super(cl);
        hidden = true;
        defaultCommand = new ProcessList(this);
        help = new HelpCommand(this, "Process Commands");
        new ProcessKill(this);
        new ProcessOutput(this);
        new ProcessExecute(this);
    }

    /**
     * Does not initialize the commands; basically acts like a blank slate.
     *
     * @param cl Command Listener instance to initialize with.
     * @param h  Used to make it hidden or not.
     *           In a future revision, this parameter will mean if it will load default commands.
     * @deprecated Signature meaning change incoming. TODO: param h == load default commands?
     */
    @Deprecated
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

    //TODO: Execute help as toDescription; requires partial rewrite, as help was never made to return message objects for other commands.

    public String toDescription(Message msg) {
        return "Process Command";
    }

    public static class ProcessKill extends AbstractCommand implements IViewable {

        public ProcessKill(ICommandListener clb) {
            super(clb);
        }

        @Override
        public void run(Message msg, String arguments) {
            getSendableChannel(msg).subscribe(c -> {
                long a;
                try {
                    var i = indexOf(arguments, Character::isSpaceChar);
                    a = Long.parseUnsignedLong(i < 0 ? arguments : arguments.substring(0, i));
                } catch (NumberFormatException nfe) {
                    c.sendMessage("Not a number.");
                    return;
                }
                var pi = Executors.INSTANCE.INSTANCES.get(a);
                if (pi == null) {
                    c.sendMessage("Process not found.");
                    return;
                }
                pi.PROCESS.destroy();
                c.sendMessage("Destroyed " + pi.PID);
            });
        }

        @Override
        public void view(Message msg) {
            //TODO: Interactive. Consider: All processes on system?
            getSendableChannel(msg).subscribe(c -> c.sendMessage("Which process would you like me to kill?"));
        }

        @Override
        public String[] toAliases() {
            return new String[]{"kill"};
        }

        @Override
        public String toDescription(Message msg) {
            return "Kills a process.\n\nUsage: `" + getStackedPrefix(LISTENER, msg.guild()) + " kill [PID]`";
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
                getSendableChannel(msg).subscribe(c -> c.sendMessage("Not a number."));
                return;
            }
            var pi = Executors.INSTANCE.INSTANCES.get(a);
            if (pi == null) {
                getSendableChannel(msg).subscribe(c -> c.sendMessage("Process not found."));
                return;
            }
            String out = pi.getOut().toString(), err = pi.getErr().toString();
            boolean $ob = out.isBlank(), $eb = err.isBlank();
            getSendableChannel(msg, VIEW_CHANNEL, SEND_MESSAGES, EMBED_LINKS, ATTACH_FILES).subscribe(mc -> {
                var eb = genBaseEmbed(0x00ff00, msg.author(), msg.guild(), "Process " + pi.PID + " streams.", null, now());
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
            });
        }

        @Override
        public String[] toAliases() {
            return new String[]{"output", "out"};
        }

        @Override
        public String toDescription(Message msg) {
            return "Gets the output from a process.\n\nUsage: `" + getStackedPrefix(LISTENER, msg.guild()) + " output [PID]`";
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
                    (pi, exit) -> {
                        String out = pi.getOut().toString(), err = pi.getErr().toString();
                        boolean $ob = out.isBlank(), $eb = err.isBlank();
                        getSendableChannel(msg, VIEW_CHANNEL, SEND_MESSAGES, EMBED_LINKS, ATTACH_FILES).subscribe(mc -> {
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
                    });
            pie.getOut().setPushListener(new Executors.DARNewline("STDOUT " + pie.PID));
            pie.getErr().setPushListener(new Executors.DARNewline("STDERR " + pie.PID));
            getSendableChannel(msg).subscribe(c -> c.sendMessage("Running " + pie.PID));
        }

        @Override
        public String[] toAliases() {
            return new String[]{"execute", "exec"};
        }

        @Override
        public String toDescription(Message msg) {
            return "Executes a process.\n\nUsage: `" + getStackedPrefix(LISTENER, msg.guild()) + " execute [Program]`";
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
                var info = i.getInfo(); //No risk of failure.
                sb.append(i.PID).append(" -> ").append(info.commandLine().orElse("Unknown?"));
            }
            if (sb.length() < 1990)
                getSendableChannel(msg).subscribe(c -> c.sendMessage(sb.insert(0, "```css\n").append("```").toString()));
            else
                getSendableChannel(msg, VIEW_CHANNEL, SEND_MESSAGES, ATTACH_FILES).subscribe(c -> c.sendMessage(attachString(new MessageOptions(), "Processes.css", sb.toString())));
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
