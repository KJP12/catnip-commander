package net.kjp12.commands.defaults.owner;

import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.message.MessageOptions;
import net.kjp12.commands.abstracts.AbstractCommand;
import net.kjp12.commands.abstracts.AbstractSubSystemCommand;
import net.kjp12.commands.abstracts.ICommandListener;
import net.kjp12.commands.abstracts.IViewable;
import net.kjp12.commands.defaults.information.HelpCommand;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.mewna.catnip.entity.util.Permission.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.kjp12.commands.utils.MiscellaneousUtils.attachString;
import static net.kjp12.commands.utils.MiscellaneousUtils.getSendableChannel;
import static net.kjp12.commands.utils.StringUtils.indexOf;

// TODO: All processes on system, or restrict to owned processes?
public class ProcessCommand extends AbstractSubSystemCommand {
    private static final Map<Long, ImmutablePair<Out, Out>> processOutput = new ConcurrentHashMap<>();

    public ProcessCommand(ICommandListener icl) {
        this(icl, true);
    }

    /**
     * @param icl          Command Listener instance to initialize with.
     * @param loadDefaults Load the default commands associated with this command?
     */
    public ProcessCommand(ICommandListener icl, boolean loadDefaults) {
        super(icl);
        if (loadDefaults) {
            defaultCommand = new ProcessList(this);
            new HelpCommand(this, "Process Commands");
            new ProcessKill(this);
            new ProcessOutput(this);
            new ProcessExecute(this);
        }
    }

    public String[] toAliases() {
        return new String[]{"process"};
    }

    public String[] toCategories() {
        return new String[]{"owner only"};
    }

    //TODO: Execute help as toDescription.

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
                ProcessHandle.of(a).ifPresentOrElse(ph -> {
                    var i = ph.info();
                    if (ProcessHandle.current().equals(ph.parent().orElse(null))) {
                        ph.destroy();
                        c.sendMessage("Killed `" + i.commandLine().orElse("Unknown Command") + "` " + ph.pid());
                    } else {
                        c.sendMessage("I don't own process `" + i.command().orElse("Unknown Command") + "`!");
                    }
                }, () -> c.sendMessage("Process not found."));
            });
        }

        @Override
        public void view(Message msg) {
            //TODO: Interactive.
            var sb = new StringBuilder("Which process would you like me to kill?\nUse '").append(LISTENER.getStackedPrefix(msg.guild())).append(" kill [PID]'\n");
            ProcessHandle.current().children().forEach(ph -> sb.append(ph.pid()).append(" -> ").append(ph.info().commandLine().orElse("Unknown?")));
            if (sb.length() < 1990) {
                getSendableChannel(msg).subscribe(c -> c.sendMessage(sb.insert(0, "```css\n").append("```").toString()));
            } else {
                getSendableChannel(msg, VIEW_CHANNEL, SEND_MESSAGES, ATTACH_FILES).subscribe(c -> c.sendMessage(attachString(new MessageOptions(), "Processes.txt", sb.toString())));
            }
        }

        @Override
        public String[] toAliases() {
            return new String[]{"kill"};
        }

        @Override
        public String toDescription(Message msg) {
            return "Kills a process.\n\nUsage: `" + LISTENER.getStackedPrefix(msg.guild()) + " kill [PID]`";
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
            var p = processOutput.get(a);
            if (p == null) {
                getSendableChannel(msg).subscribe(c -> c.sendMessage("Process output not found."));
            } else {
                Out bytesOut = p.left, bytesErr = p.right;
                getSendableChannel(msg, VIEW_CHANNEL, SEND_MESSAGES, EMBED_LINKS, ATTACH_FILES).subscribe(mc -> {
                    var mo = new MessageOptions().content("Process output streams for " + a + '.');
                    if (!bytesOut.isBlank()) mo.addFile("Out-" + a + ".log", bytesOut.trimmedArray());
                    if (!bytesErr.isBlank()) mo.addFile("Err-" + a + ".log", bytesErr.trimmedArray());
                    mc.sendMessage(mo);
                });
            }
        }

        @Override
        public String[] toAliases() {
            return new String[]{"output", "out"};
        }

        @Override
        public String toDescription(Message msg) {
            return "Gets the output from a process.\n\nUsage: `" + LISTENER.getStackedPrefix(msg.guild()) + " output [PID]`";
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
            var p = Runtime.getRuntime().exec(arguments);
            var pid = p.pid();
            Out bytesOut = new Out(1048576), bytesErr = new Out(1048576);
            Thread stdout = new Thread("STDOUT " + pid) {
                @Override
                public void run() {
                    try (var in = p.getInputStream()) {
                        var arr = new byte[1024];
                        int i;
                        while ((i = in.read(arr)) >= 0) bytesOut.write(arr, i);
                    } catch (IOException ioe) {
                        LISTENER.handleThrowable(ioe, msg);
                    }
                }
            }, stderr = new Thread("STDERR " + pid) {
                @Override
                public void run() {
                    try (var in = p.getErrorStream()) {
                        var arr = new byte[1024];
                        int i;
                        while ((i = in.read(arr)) >= 0) bytesErr.write(arr, i);
                    } catch (IOException ioe) {
                        LISTENER.handleThrowable(ioe, msg);
                    }
                }
            }, stddie = new Thread("DEATH " + pid) {
                @Override
                public void run() {
                    var s = new ArrayList<Throwable>();
                    int exit = -1;
                    try {
                        exit = p.waitFor();
                    } catch (InterruptedException e) {
                        s.add(e);
                    }
                    try {
                        stderr.join();
                    } catch (InterruptedException e) {
                        s.add(e);
                    }
                    try {
                        stdout.join();
                    } catch (InterruptedException e) {
                        s.add(e);
                    }
                    if (!s.isEmpty()) {
                        var t = new Exception("Error trying to wait for threads!");
                        for (var b : s) t.addSuppressed(b);
                        LISTENER.handleThrowable(t, msg);
                    }
                    final int finalExit = exit;
                    processOutput.remove(pid);
                    getSendableChannel(msg, VIEW_CHANNEL, SEND_MESSAGES, EMBED_LINKS, ATTACH_FILES).subscribe(mc -> {
                        var mo = new MessageOptions().content("Process " + pid + " exited with " + finalExit + '.');
                        if (!bytesOut.isBlank()) mo.addFile("Out-" + pid + ".log", bytesOut.trimmedArray());
                        if (!bytesErr.isBlank()) mo.addFile("Err-" + pid + ".log", bytesErr.trimmedArray());
                        mc.sendMessage(mo);
                    });
                }
            };
            processOutput.put(pid, ImmutablePair.of(bytesOut, bytesErr));
            getSendableChannel(msg).subscribe(c -> c.sendMessage("Running " + pid));
            stdout.start();
            stderr.start();
            stddie.start();
        }

        @Override
        public String[] toAliases() {
            return new String[]{"execute", "exec"};
        }

        @Override
        public String toDescription(Message msg) {
            return "Executes a process.\n\nUsage: `" + LISTENER.getStackedPrefix(msg.guild()) + " execute [Program]`";
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
            ProcessHandle.current().children().forEach(ph -> sb.append(ph.pid()).append(" -> ").append(ph.info().commandLine().orElse("Unknown?")));
            if (sb.length() < 1990)
                getSendableChannel(msg).subscribe(c -> c.sendMessage(sb.insert(0, "```css\n").append("```").toString()));
            else
                getSendableChannel(msg, VIEW_CHANNEL, SEND_MESSAGES, ATTACH_FILES).subscribe(c -> c.sendMessage(attachString(new MessageOptions(), "Processes.txt", sb.toString())));
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

    public static class Out {
        public final byte[] data;
        public int index;

        public Out(int len) {
            data = new byte[len];
        }

        public void write(byte[] in, int i) {
            var b = index + i - data.length;
            if (b > 0) {
                System.arraycopy(data, b, data, 0, index);
                index -= b;
            }
            System.arraycopy(in, 0, data, index, i);
            index += i;
        }

        public String toString() {
            return new String(data, 0, index, UTF_8);
        }

        public byte[] trimmedArray() {
            int s = 0, e = index;
            while (s < index && Character.isWhitespace(data[s])) s++;
            while (e > s && Character.isWhitespace(data[e])) e--;
            return Arrays.copyOfRange(data, s, e);
        }

        public boolean isBlank() {
            for (int i = 0; i < index; i++) if (!Character.isWhitespace(data[i])) return false;
            return true;
        }
    }
}
