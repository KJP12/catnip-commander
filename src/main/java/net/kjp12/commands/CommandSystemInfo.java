package net.kjp12.commands;//Created on 05/10/18.

public final class CommandSystemInfo {
    public static final String MAJOR = "@major@", MINOR = "@minor@", REVISION = "@revision@", BUILD = "@build@", VERSION = BUILD.indexOf("build") == 1 ? "Live Debug Environment" : MAJOR + '.' + MINOR + '.' + REVISION + '_' + BUILD;

    private CommandSystemInfo() {
    }
}
