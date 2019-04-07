@file:JvmName("StringUtils")

package net.kjp12.commands.utils

import com.mewna.catnip.entity.guild.Guild
import com.mewna.catnip.entity.user.User
import java.util.function.IntPredicate

fun Any?.stringify() = if (this == null) "null" else "${this.javaClass.name}@${Integer.toHexString(System.identityHashCode(this))}$${Integer.toHexString(hashCode())}"

fun Guild.stringify(id: Boolean = true) = if (id) "${name()} ${idAsLong()}" else name()

fun User.stringify(id: Boolean = true) = if (id) "${discordTag()} ${idAsLong()}" else discordTag()

@JvmOverloads
fun String.splitByPredicate(t: IntPredicate, start: Int = 0, limit: Int) = StringUtilsJava.splitByPredicate(t, start, limit, this)

@JvmOverloads
fun String.indexOf(t: IntPredicate, start: Int = 0): Int {
    var i = start
    while (i < length && !t.test(this[i].toInt())) i++
    return i
}

fun String.countInstances(t: (Char) -> Boolean, start: Int): Int {
    if (isEmpty() || start >= length) return 0
    var c = 0
    var it = false
    var i = start
    while (i < length) {
        if (t(this[i])) it = true
        else if (it) {
            c++
            it = false
        }
        i++
    }
    return if (it) c + 1 else c
}