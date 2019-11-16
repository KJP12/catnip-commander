package net.kjp12.commands.utils;

import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.user.User;

import java.util.function.IntPredicate;

public final class StringUtils {
    private StringUtils() {
    }

    public static String stringify(Object o) {
        if (o == null) return "null";
        return o.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(o)) + '$' + Integer.toHexString(o.hashCode());
    }

    public static String stringify(Guild g, boolean id) {
        if (id) return g.name() + ' ' + g.idAsLong();
        return g.name();
    }

    public static String stringify(User u, boolean id) {
        if (id) return u.discordTag() + ' ' + u.idAsLong();
        return u.discordTag();
    }

    public static String[] splitByPredicate(String toSplit, IntPredicate t) {
        return splitByPredicate(toSplit, t, 0, -1);
    }

    public static String[] splitByPredicate(String toSplit, IntPredicate t, int start) {
        return splitByPredicate(toSplit, t, start, -1);
    }

    @SuppressWarnings("StatementWithEmptyBody") //For-loops are supposed to be empty.
    public static String[] splitByPredicate(String toSplit, IntPredicate t, int start, int limit) {
        while (start < toSplit.length() && t.test(toSplit.charAt(start)))
            start++;
        String[] arr = new String[toSplit.length() <= start ? 0 :
                limit < 0 ? countInstances(toSplit, t, start) + 1 :
                        Math.min(countInstances(toSplit, t, start) + 1, limit)];
        if (arr.length == 1) arr[0] = start > 0 ? toSplit.substring(start) : toSplit;
        if (arr.length <= 1) return arr;
        int a = start, b = start, d = 0, e;
        for (; a < toSplit.length() && d < arr.length - 1; a++) {
            if (t.test(toSplit.charAt(a))) {
                for (e = a - 1; e >= b && t.test(toSplit.charAt(e)); e--) ;
                for (; b < toSplit.length() && b < a && (b < 0 || t.test(toSplit.charAt(b))); b++) ;
                arr[d++] = toSplit.substring(b, e + 1);
                b = a;
            }
        }
        for (e = toSplit.length() - 1; e >= b && t.test(toSplit.charAt(e)); e--) ;
        for (a = a >= toSplit.length() ? b : a; a < toSplit.length() && t.test(toSplit.charAt(a)); a++) ;
        arr[d++] = toSplit.substring(a, e + 1);
        if (d != arr.length)
            throw new AssertionError("expected split to equal array, len was " + d + " while array was " + arr.length);
        return arr;
    }

    public static int indexOf(String s, IntPredicate t) {
        return indexOf(s, t, 0);
    }

    public static int indexOf(String s, IntPredicate t, int start) {
        var i = start;
        while (i < s.length() && !t.test(s.charAt(i))) i++;
        return i == s.length() ? -1 : i;
    }

    /*@JvmOverloads
    fun String.indexOf(t: IntPredicate, start: Int = 0): Int {
        var i = start
        while (i < length && !t.test(this[i].toInt())) i++
        return if (i == length) -1 else i
    }*/

    public static int countInstances(String s, IntPredicate t) {
        return countInstances(s, t, 0);
    }

    public static int countInstances(String s, IntPredicate t, int start) {
        if (s.isEmpty() || start >= s.length()) return 0;
        var c = 0;
        var it = false;
        for (var i = start; i < s.length(); i++) {
            if (t.test(s.charAt(i))) it = true;
            else if (it) {
                c++;
                it = false;
            }
        }
        return it ? c + 1 : c;
    }

    /*@JvmOverloads
    fun String.countInstances(t: (Char) -> Boolean, start: Int = 0): Int {
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
    }*/
}
