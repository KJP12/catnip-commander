package net.kjp12.commands.utils;

import org.junit.jupiter.api.Test;

import static net.kjp12.commands.utils.MiscellaneousUtils.getOrDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SuppressWarnings("RedundantTypeArguments") // Ambiguous to the compiler.
public class MiscellaneousUtilsJavaTest {
    final Object obj1 = new Object(), obj2 = new Object();

    {
        assertNotEquals(obj1, obj2, "Initialization Error");
    }

    @Test
    public void getOrDefaultStringThrowingPredefined() {
        assertEquals("This is a predefined string.", getOrDefault(() -> {
            throw new Exception("This should be swallowed.");
        }, "This is a predefined string."));
    }

    @Test
    public void getOrDefaultStringThrowingSupplied() {
        assertEquals("This is a supplied string.", MiscellaneousUtils.<String>getOrDefault(() -> {
            throw new Exception("This should be swallowed.");
        }, () -> "This is a supplied string."));
    }

    @Test
    public void getOrDefaultStringReturningPredefined() {
        assertEquals("Predefined suppressed.", getOrDefault(() -> "Predefined suppressed.", "This is a predefined string."));
    }

    @Test
    public void getOrDefaultStringReturningSupplied() {
        assertEquals("Supplier suppressed.", MiscellaneousUtils.<String>getOrDefault(() -> "Supplier suppressed.", () -> "This is a supplied string."));
    }

    @Test
    public void getOrDefaultObjectThrowingPredefined() {
        assertEquals(obj1, getOrDefault(() -> {
            throw new Exception("This should be swallowed.");
        }, obj1));
    }

    @Test
    public void getOrDefaultObjectThrowingSupplied() {
        assertEquals(obj1, MiscellaneousUtils.<Object>getOrDefault(() -> {
            throw new Exception("This should be swallowed.");
        }, () -> obj1));
    }

    @Test
    public void getOrDefaultObjectReturningPredefined() {
        assertEquals(obj2, getOrDefault(() -> obj2, obj1));
    }

    @Test
    public void getOrDefaultObjectReturningSupplied() {
        assertEquals(obj2, MiscellaneousUtils.<Object>getOrDefault(() -> obj2, () -> obj1));
    }
}
