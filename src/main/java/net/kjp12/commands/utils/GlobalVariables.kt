@file:JvmName("GlobalVariables")

package net.kjp12.commands.utils

import java.io.PrintWriter
import java.nio.charset.Charset

@JvmField val STDOUT_WRITER: PrintWriter = PrintWriter(System.out)
@JvmField val STDERR_WRITER: PrintWriter = PrintWriter(System.err)
@JvmField var charset: Charset = Charsets.UTF_8