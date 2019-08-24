package net.kjp12.commands.utils;

import org.junit.jupiter.api.Test;

import static net.kjp12.commands.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringUtilsJavaTest {
    private final String str = "This should only count\r5 instances.";

    @Test
    public void countInstancesNoSkip() {
        assertEquals(5, countInstances(str, Character::isWhitespace));
    }

    @Test
    public void countInstancesSkip4() {
        assertEquals(5, countInstances(str, Character::isWhitespace, 4));
    }

    @Test
    public void countInstancesSkip5() {
        assertEquals(4, countInstances(str, Character::isWhitespace, 5));
    }

    @Test
    public void countInstancesSkip10() {
        assertEquals(4, countInstances(str, Character::isWhitespace, 10));
    }

    @Test
    public void indexOf() {
        int[] arr = {4, 11, 16, 22, 24, -1};
        int i = 0, p = 0;
        while (p > -1) {
            assertEquals(arr[i], p = StringUtils.indexOf(str, Character::isWhitespace, p + 1), "Expected " + arr[i] + " at " + i + " of " + str);
            i++;
        }
    }

    @Test
    public void splitByPredicateNoLimit() {
        String[] arr = {"This", "should", "only", "count", "5", "instances."};
        var tmp = splitByPredicate(str, Character::isWhitespace);
        assertArrayEquals(arr, tmp);
    }

    @Test
    public void splitByPredicateStart4() {
        String[] arr = {"should", "only", "count", "5", "instances."};
        var tmp = splitByPredicate(str, Character::isWhitespace, 4);
        assertArrayEquals(arr, tmp);
    }

    @Test
    public void splitByPredicateStart5() {
        String[] arr = {"should", "only", "count", "5", "instances."};
        var tmp = splitByPredicate(str, Character::isWhitespace, 5);
        assertArrayEquals(arr, tmp);
    }

    @Test
    public void splitByPredicateStart6() {
        String[] arr = {"hould", "only", "count", "5", "instances."};
        var tmp = splitByPredicate(str, Character::isWhitespace, 6);
        assertArrayEquals(arr, tmp);
    }

    @Test
    public void splitByPredicateLimit5() {
        String[] arr = {"This", "should", "only", "count", "5 instances."};
        var tmp = splitByPredicate(str, Character::isWhitespace, 0, 5);
        assertArrayEquals(arr, tmp);
    }

    @Test
    public void splitByPredicateLimit3() {
        String[] arr = {"This", "should", "only count\r5 instances."};
        var tmp = splitByPredicate(str, Character::isWhitespace, 0, 3);
        assertArrayEquals(arr, tmp);
    }

    @Test
    public void splitByPredicateStart6Limit3() {
        String[] arr = {"hould", "only", "count\r5 instances."};
        var tmp = splitByPredicate(str, Character::isWhitespace, 6, 3);
        assertArrayEquals(arr, tmp);
    }

    @Test
    public void nullStringify() {
        assertEquals("null", stringify(null));
    }

    @Test
    public void objStringify() {
        var obj = new Object();
        var oh = obj.hashCode();
        var sh = System.identityHashCode(obj);
        System.out.println("Proposed Hash: $oh; System Hash: $sh");
        assertEquals("java.lang.Object@" + Integer.toHexString(sh) + '$' + Integer.toHexString(oh), stringify(obj));
    }

    @Test
    public void rigStringify() {
        class Rig {
            public int hashCode() {
                return 1337;
            }

            public boolean equals(Object other) {
                return other instanceof Rig;
            }
        }
        var obj = new Rig();
        var sh = System.identityHashCode(obj);
        System.out.println("Proposed Hash: 1337; System Hash: " + sh);
        assertEquals("net.kjp12.commands.utils.StringUtilsJavaTest$1Rig@" + Integer.toHexString(sh) + '$' + Integer.toHexString(1337), stringify(obj));
    }
}
