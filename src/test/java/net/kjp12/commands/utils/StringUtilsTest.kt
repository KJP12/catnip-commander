package net.kjp12.commands.utils

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.function.IntPredicate

class StringUtilsTest {
    private val str = "This should only count\r5 instances."

    @Test fun countInstancesNoSkip() = assertEquals(5, str.countInstances({ Character.isWhitespace(it) }))

    @Test fun countInstanceSkip4() = assertEquals(5, str.countInstances({ Character.isWhitespace(it) }, 4))

    @Test fun countInstanceSkip5() = assertEquals(4, str.countInstances({ Character.isWhitespace(it) }, 5))

    @Test fun countInstanceSkip10() = assertEquals(4, str.countInstances({ Character.isWhitespace(it) }, 10))

    @Test
    fun indexOf() {
        val arr = arrayOf(4, 11, 16, 22, 24, -1)
        var i = 0
        var p = 0
        while (p > -1) {
            p = str.indexOf(IntPredicate { Character.isWhitespace(it.toChar()) }, p + 1)
            assertEquals(arr[i], p) { "Expected ${arr[i]} @ $i of $str" }
            i++
        }
    }

    @Test
    fun splitByPredicate() {
        val arr = arrayOf("This", "should", "only", "count", "5", "instances.")
        val tmp = str.splitByPredicate(IntPredicate { Character.isWhitespace(it.toChar()) })
        assertArrayEquals(arr, tmp)
    }

    @Test
    fun splitByPredicateStart4() {
        val arr = arrayOf("should", "only", "count", "5", "instances.")
        val tmp = str.splitByPredicate(IntPredicate { Character.isWhitespace(it.toChar()) }, 4)
        assertArrayEquals(arr, tmp)
    }

    @Test
    fun splitByPredicateStart5() {
        val arr = arrayOf("should", "only", "count", "5", "instances.")
        val tmp = str.splitByPredicate(IntPredicate { Character.isWhitespace(it.toChar()) }, 5)
        assertArrayEquals(arr, tmp)
    }

    @Test
    fun splitByPredicateStart6() {
        val arr = arrayOf("hould", "only", "count", "5", "instances.")
        val tmp = str.splitByPredicate(IntPredicate { Character.isWhitespace(it.toChar()) }, 6)
        assertArrayEquals(arr, tmp)
    }

    @Test
    fun splitByPredicateLimit5() {
        val arr = arrayOf("This", "should", "only", "count", "5 instances.")
        val tmp = str.splitByPredicate(IntPredicate { Character.isWhitespace(it.toChar()) }, limit = 5)
        assertArrayEquals(arr, tmp)
    }

    @Test
    fun splitByPredicateLimit3() {
        val arr = arrayOf("This", "should", "only count\r5 instances.")
        val tmp = str.splitByPredicate(IntPredicate { Character.isWhitespace(it.toChar()) }, limit = 3)
        assertArrayEquals(arr, tmp)
    }

    @Test
    fun splitByPredicateStart6Limit3() {
        val arr = arrayOf("hould", "only", "count\r5 instances.")
        val tmp = str.splitByPredicate(IntPredicate { Character.isWhitespace(it.toChar()) }, 6, 3)
        assertArrayEquals(arr, tmp)
    }

    @Test fun nullStringify() = assertEquals("null", null.stringify())

    @Test fun objStringify() {
        val obj: Any = Object()
        val oh = obj.hashCode()
        val sh = System.identityHashCode(obj)
        println("Proposed Hash: $oh; System Hash: $sh")
        assertEquals("java.lang.Object@${Integer.toHexString(sh)}$${Integer.toHexString(oh)}", obj.stringify())
    }

    @Test
    fun rigStringify() {
        class Rig {
            override fun hashCode() = 1337
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                return true
            }
        }

        val rig = Rig()
        val sh = System.identityHashCode(rig)
        println("Proposed Hash: 1337; System Hash: $sh")
        assertEquals("net.kjp12.commands.utils.StringUtilsTest\$rigStringify\$Rig@${Integer.toHexString(sh)}$${Integer.toHexString(1337)}", rig.stringify())
    }
}