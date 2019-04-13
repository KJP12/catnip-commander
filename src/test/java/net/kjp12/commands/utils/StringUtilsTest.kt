package net.kjp12.commands.utils

import org.junit.jupiter.api.Test
import java.util.*
import java.util.function.IntPredicate

class StringUtilsTest {
    private val str = "This should only count\r5 instances"

    @Test
    fun countInstances() {
        var c = str.countInstances({ Character.isWhitespace(it) })
        assert(c == 5) { "Expected 5, got $c" }
        c = str.countInstances({ Character.isWhitespace(it) }, 10)
        assert(c == 4) { "Expected 5 after 10 chars skipped, got $c" }
    }

    @Test
    fun indexOf() {
        val arr = arrayOf(4, 11, 16, 22, 24, -1)
        var i = 0
        var p = 0
        while (p > -1) {
            p = str.indexOf(IntPredicate { Character.isWhitespace(it.toChar()) }, p + 1)
            assert(arr[i] == p) { "Expected ${arr[i]} @ $i, got $p" }
            i++
        }
    }

    @Test
    fun splitByPredicate() {
        val str = "This should split into\r6 strings."
        val arr = arrayOf("This", "should", "split", "into", "6", "strings.")
        val tmp = str.splitByPredicate(IntPredicate { Character.isWhitespace(it.toChar()) })
        assert(tmp.size == 6) { "Expected length of 6, got ${tmp.size}" }
        assert(Arrays.equals(arr, tmp)) { "Expected ${Arrays.toString(arr)}, got ${Arrays.toString(tmp)}" }
    }

    @Test
    fun stringify() {
        var tmp = null.stringify()
        assert(tmp == "null") { "Expected \"null\", got \"$tmp\"" }
        var obj: Any = Object()
        tmp = obj.stringify()
        var oh = obj.hashCode()
        var sh = System.identityHashCode(obj)
        println("Proposed Hash: $oh; System Hash: $sh")
        assert(tmp == "java.lang.Object@${Integer.toHexString(sh)}$${Integer.toHexString(oh)}") { "Standard-Hash Output doesn't match expected string" }
        class oj {
            override fun hashCode() = 1337
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                return true
            }
        }
        obj = oj()
        tmp = obj.stringify()
        sh = System.identityHashCode(obj)
        println("Proposed Hash: 1337; System Hash: $sh")
        assert(
            tmp == "net.kjp12.commands.utils.StringUtilsTest\$stringify\$oj@${Integer.toHexString(sh)}$${Integer.toHexString(
                1337
            )}"
        ) { "Override-Hash Output doesn't match expected string" }
    }
}