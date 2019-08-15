package net.kjp12.commands;

import com.koloboke.collect.map.LongObjMap;
import com.koloboke.collect.map.hash.HashLongObjMaps;
import net.kjp12.commands.utils.GlobalVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class Executors extends ThreadGroup {
    public static final Executors INSTANCE = new Executors();
    private static final Runtime RT = Runtime.getRuntime();
    private static final File CURRENT_DIR = new File(".");
    private static final Logger LOGGER = LoggerFactory.getLogger(Executors.class);
    private static final AtomicLong INCREMENT = new AtomicLong(0);
    private final LongObjMap<ProcessInstance> INSTANCES_ = HashLongObjMaps.newMutableMap();
    public final Map<Long, ProcessInstance> INSTANCES = Collections.unmodifiableMap(INSTANCES_);

    private Executors() {
        super("Executors");
        setMaxPriority(Thread.NORM_PRIORITY - 1); //isn't crucial for operation.
    }

    @SafeVarargs
    public final ProcessInstance execute(String process, Consumer<Throwable> error, DurianBiConsumer<ProcessInstance, Integer>... exit) throws IOException {
        return execute(process, CURRENT_DIR, error, exit);
    }

    @SafeVarargs
    public final ProcessInstance execute(String process, File workDir, Consumer<Throwable> error, DurianBiConsumer<ProcessInstance, Integer>... exit) throws IOException {
        return new ProcessInstance(process, workDir, error, exit);
    }

    public Thread generateReadingThread(String name, InputStream is, DynamicArrayReader dar) {
        return new Thread(Executors.this, name) {
            public void run() {
                Reader reader = null;
                try {
                    reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                    char[] charBuffer = new char[8192];
                    //noinspection StatementWithEmptyBody
                    while (dar.push(charBuffer, reader.read(charBuffer))) ;
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                } finally {
                    if (reader != null) try {
                        reader.close();
                    } catch (IOException ioe) {
                        LOGGER.error("Error closing on " + name, ioe);
                    }
                }
            }
        };
    }

    @FunctionalInterface
    public interface DARFunction {
        void run(char[] buf, boolean closed, int ri, int wi, int m);
    }

    public interface DurianBiConsumer<A, B> {
        void accept(A a, B b) throws Throwable;
    }

    /**
     * Destructive in nature, if anything goes unread, chances are, it might be cycled over.
     */
    @SuppressWarnings("SynchronizeOnNonFinalField")
    public static class DynamicArrayReader extends Reader {
        protected final char[] arr;
        protected volatile boolean closed;
        protected volatile DARFunction func;
        protected int wi, ri;//write index, read index
        protected long skipIndex;

        private DynamicArrayReader(int arrSize) {
            super(new char[arrSize]);
            arr = (char[]) lock;
        }

        /**
         * @param tmp char array. Assumed to be 0-start.
         * @param l   Amount to read from said array.
         * @return l >= 0
         **/
        private boolean push(char[] tmp, int l) {
            if (l < 0) {
                //Symbolically closes the stream.
                synchronized (lock) {
                    closed = true;
                    if (wi > 0) func.run(arr, true, ri, wi, 0);
                    lock.notifyAll();
                }
                synchronized (this) {
                    //ensures that everything gets notified properly.
                    notifyAll();
                }
                return false;
            }
            if (l > 0) synchronized (lock) {
                int a = (wi + l) - arr.length;
                if (a > 0) System.arraycopy(arr, a, arr, 0, wi -= a);
                System.arraycopy(tmp, 0, arr, wi, l);
                wi += l;
                updateSkip(-l);
                if (func != null) func.run(arr, closed, ri, wi, Math.max(a, 0));
                lock.notifyAll();
            }
            return true;
        }

        public void setPushListener(DARFunction func) {
            this.func = func;
            if (wi > 0) func.run(arr, closed, ri, wi, 0);
        }

        /**
         * Expected to be in a synchronized; this will not synchronize itself.
         *
         * @param l If positive, is how much to skip. If negative, how much to read.
         * @return How much is currently readable until {@link #wi}.
         */
        private long updateSkip(long l) {
            if (l > 0) {
                if (skipIndex == 0) {
                    int i = wi - ri;
                    skipIndex = l - i;
                    ri = wi;
                    if (skipIndex < 0) {
                        ri += (int) skipIndex;
                        if (ri < 0) ri = 0;
                        skipIndex = 0;
                    }
                } else {
                    long tmp;
                    skipIndex = ((tmp = skipIndex + l) < 0) ? Long.MAX_VALUE : tmp;
                }
                return l; //in a sense, we did skip that many.
            } else if (l < 0) {
                if (skipIndex >= -l) {
                    skipIndex += l;
                    return 0;
                } else if (skipIndex > 0) {
                    ri -= l - skipIndex;
                    skipIndex = 0;
                }
            }
            return wi - ri;
        }

        public int read(@Nonnull CharBuffer target) {
            if (lock == null) return -1;
            synchronized (lock) {
                int len = (int) updateSkip(-target.remaining());
                target.put(arr, ri, len);
                ri += len;
                return len;
            }
        }

        public int read() {
            return ri >= wi ? -1 : arr[ri++];
        }

        /*public int read(char[] cbuf) throws IOException {
            return this.read(cbuf, 0, cbuf.length);
        }*/

        public long skip(long n) {
            if (n < 0L) {
                throw new IllegalArgumentException("skip value is negative");
            } else synchronized (lock) {
                //as we are just an array, we don't need to use the buffer mechanics.
                return updateSkip(n);
            }
        }

        public boolean ready() {
            return ri < wi;
        }

        @Override
        public int read(@Nonnull char[] chars, int i, int i1) throws IOException {
            if (i > chars.length) throw new ArrayIndexOutOfBoundsException("index > " + chars.length);
            if (ri >= wi && closed) return -1;
            synchronized (lock) {
                //Shortens the length to the underlying array if applicable.
                i1 = Math.min(chars.length - i, i1);
                int total = 0, len;
                while (i1 > 0 && !closed) {
                    total += len = Math.min(i1, wi - ri);
                    if (len > 0) {
                        System.arraycopy(arr, ri, chars, i, len);
                        ri += len;
                        i += len;
                        i1 -= len;
                    } else try {
                        lock.wait();
                    } catch (InterruptedException ie) {
                        throw new InterruptedIOException(ie.getMessage());
                    }
                }
                return total;
            }
        }

        public long transferTo(Writer out) throws IOException {
            if (ri >= wi && closed) return -1;
            synchronized (lock) {
                long total = 0;
                int len;
                while (ri < wi && !closed) {
                    total += len = wi - ri;
                    out.write(arr, ri, len);
                    ri += len;
                }
                return total;
            }
        }

        //We cannot reset far, but it can be reset back to the beginning of the buffer.
        //TODO: Support marking?
        public void reset() {
            ri = 0;
        }

        @Override
        public void close() {
            //This cannot ever close, as we have nothing to release.
            //And the other end cannot close it either. So, closing does absolutely nothing.
        }

        public String toString() {
            return String.valueOf(arr, 0, wi);
        }
    }

    public static final class DARNewline implements DARFunction {
        private final String name;
        int si = 0;

        public DARNewline(String name) {
            this.name = name;
        }

        @Override
        public synchronized void run(char[] buf, boolean closed, int ri, int wi, int m) {
            int i;
            for (i = Math.max(si -= m, 0); i < wi; i++) {
                if (buf[i] == '\n') {
                    GlobalVariables.STDOUT_WRITER.append(name).append(' ').write(buf, si, i - si);
                    GlobalVariables.STDOUT_WRITER.append('\n').flush();
                    si = i + 1;
                }
            }
            if (closed) {
                GlobalVariables.STDOUT_WRITER.append(name).append(' ').write(buf, si, i - si);
                GlobalVariables.STDOUT_WRITER.append('\n').flush();
            }
        }
    }

    public class ProcessInstance {
        private static final int SIZE = 4 << 20;
        public final long PID, ns = System.nanoTime();
        public final Process PROCESS;
        private ProcessHandle handle; //If this doesn't exist, assume no native support and use fallback info behaviour.
        private ProcessHandle.Info info; //This will act as cache, changing behaviour slightly; may hopefully remove "/usr/bin/bash /path/to/executable" behaviour on *NIX.
        private final DynamicArrayReader OUT_S = new DynamicArrayReader(SIZE), ERR_S = new DynamicArrayReader(SIZE);
        private final Thread DEATH, ERR_T, OUT_T;
        private volatile long nsd = 0;

        private ProcessInstance(String process, File dir, Consumer<Throwable> errors, DurianBiConsumer<ProcessInstance, Integer>[] listeners) throws IOException {
            if (process.isEmpty()) throw new IllegalArgumentException("process blank");
            var st = new StringTokenizer(process);
            var cmdarray = new String[st.countTokens()];
            for (int i = 0; st.hasMoreTokens(); ++i) cmdarray[i] = st.nextToken();
            PROCESS = RT.exec(cmdarray, null, dir);
            long $pid = 0;
            try {
                handle = PROCESS.toHandle();
                $pid = handle.pid();
                info = handle.info();
            } catch (UnsupportedOperationException uoe) {
                LOGGER.warn("Couldn't use JVM Process Info; generating own using available information.", uoe);
                //We'll just make our own in this case.
                info = new ProcessHandle.Info() {
                    private final String[] ARGS = Arrays.copyOfRange(cmdarray, 1, cmdarray.length);
                    private final String cmd = cmdarray[0], user = System.getProperty("user.name");

                    @Override
                    public Optional<String> command() {
                        return Optional.of(cmd);
                    }

                    @Override
                    public Optional<String> commandLine() {
                        return Optional.of(process);
                    }

                    @Override
                    public Optional<String[]> arguments() {
                        return Optional.of(Arrays.copyOf(ARGS, ARGS.length));
                    }

                    @Override
                    public Optional<Instant> startInstant() {
                        return Optional.of(Instant.ofEpochSecond(0, ns));
                    }

                    @Override
                    public Optional<Duration> totalCpuDuration() {
                        return Optional.of(Duration.ofNanos((nsd == 0 ? System.nanoTime() : nsd) - ns));
                    }

                    @Override
                    public Optional<String> user() {
                        // We will assume it's the same as the one in System. This is generally not a good assumption,
                        // but it's assumed that the process doesn't elevate itself to root or run under a different user.
                        return Optional.ofNullable(user);
                    }
                };
            } finally {
                if ($pid == 0) {
                    $pid = INCREMENT.get();
                    while (INSTANCES_.containsKey($pid)) $pid++;
                    INCREMENT.set($pid + 1L);
                }
                PID = $pid;
                synchronized (INSTANCES_) {
                    INSTANCES_.put(PID, this);
                }
            }
            DEATH = new Thread(Executors.this, "DEATH Process " + PID) {
                public void run() {
                    int exit = -1;
                    try {
                        exit = PROCESS.waitFor();
                    } catch (InterruptedException ignore) {
                    } finally {
                        nsd = System.nanoTime();
                        LOGGER.debug("Time to execute {} {} @ {} was {}ns", process, PID, dir, nsd - ns);
                        if (!OUT_S.closed) try {
                            LOGGER.trace("STDOUT for {} wasn't fully read!?", PID);
                            synchronized (OUT_S) {
                                OUT_S.wait();
                            }
                        } catch (InterruptedException ignore) {
                        }
                        if (!ERR_S.closed) try {
                            LOGGER.trace("STDERR for {} wasn't fully read!?", PID);
                            synchronized (ERR_S) {
                                ERR_S.wait();
                            }
                        } catch (InterruptedException ignore) {
                        }
                        synchronized (INSTANCES_) {
                            INSTANCES_.remove(PID);
                            if (INSTANCES_.containsKey(PID)) //noinspection ThrowFromFinallyBlock
                                throw new InternalError(PID + " not removed?!");
                        }
                        for (var d : listeners) {
                            try {
                                d.accept(ProcessInstance.this, exit);
                            } catch (Throwable t) {
                                LOGGER.error("Error on process " + PID, t);
                                errors.accept(t);
                            }
                        }
                    }
                }
            };
            DEATH.start();
            OUT_T = generateReadingThread("STDOUT " + PID, PROCESS.getInputStream(), OUT_S);
            ERR_T = generateReadingThread("STDERR " + PID, PROCESS.getErrorStream(), ERR_S);
            OUT_T.start();
            ERR_T.start();
        }

        public DynamicArrayReader getErr() {
            return ERR_S;
        }

        public DynamicArrayReader getOut() {
            return OUT_S;
        }

        public ProcessHandle.Info getInfo() {
            if (handle == null) return info;
            var i = handle.info();
            return i.command().isEmpty() ? info : (info = i);
        }
    }
}
