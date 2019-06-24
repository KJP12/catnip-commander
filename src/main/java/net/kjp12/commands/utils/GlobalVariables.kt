@file:JvmName("GlobalVariables")

package net.kjp12.commands.utils

import java.io.PrintWriter
import java.nio.charset.Charset

@JvmField
val STDOUT_WRITER: PrintWriter = PrintWriter(System.out)
@JvmField
val STDERR_WRITER: PrintWriter = PrintWriter(System.err)
@JvmField @Deprecated("Unneeded", ReplaceWith("StandardCharsets.UTF_8"), DeprecationLevel.ERROR) @java.lang.Deprecated(forRemoval = true)
var charset: Charset = Charsets.UTF_8