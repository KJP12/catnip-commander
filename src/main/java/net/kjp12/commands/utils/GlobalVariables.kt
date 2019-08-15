@file:JvmName("GlobalVariables")

package net.kjp12.commands.utils

import java.io.PrintWriter

@JvmField
val STDOUT_WRITER: PrintWriter = PrintWriter(System.out)
@JvmField
val STDERR_WRITER: PrintWriter = PrintWriter(System.err)