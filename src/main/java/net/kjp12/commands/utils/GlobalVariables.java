package net.kjp12.commands.utils;

import java.io.PrintWriter;

@Deprecated(forRemoval = true)
public final class GlobalVariables {
    public static final PrintWriter STDOUT_WRITER = new PrintWriter(System.out),
            STDERR_WRITER = new PrintWriter(System.err);

    private GlobalVariables() {
    }
}
