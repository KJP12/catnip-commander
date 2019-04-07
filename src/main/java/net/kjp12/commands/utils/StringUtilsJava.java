package net.kjp12.commands.utils;

import java.util.Arrays;
import java.util.function.IntPredicate;

import static net.kjp12.commands.utils.StringUtils.countInstances;

class StringUtilsJava {
    static String[] splitByPredicate(IntPredicate t, int start, int limit, String toSplit) {
        while (start < toSplit.length() && t.test(toSplit.charAt(start)))
            start++;
        String[] arr = new String[toSplit.length() <= start ? 0 :
                limit < 0 ? countInstances(toSplit, t::test, start) + 1 :
                        Math.min(countInstances(toSplit, t::test, start) + 1, limit)];
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
}
