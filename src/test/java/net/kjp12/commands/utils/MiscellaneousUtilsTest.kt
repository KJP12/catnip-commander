package net.kjp12.commands.utils

import org.junit.jupiter.api.Test
import java.util.concurrent.Callable
import java.util.function.Supplier

class MiscellaneousUtilsTest {
    @Test
    fun getOrDefault() {
        var t = Callable<String> { throw Exception("This should be swallowed") }.getOrDefault("Something was thrown")
        assert(t == "Something was thrown") { "Always-Throw Predefined returned \"$t\"" }
        t = Callable<String> { throw Exception("This should be swallowed") }.getOrDefault(Supplier { "Something was definitely thrown; I'm an auto-generated stub." })
        assert(t == "Something was definitely thrown; I'm an auto-generated stub.") { "Always-Throw Expensive returned \"$t\"" }
        t = Callable<String> { "Something" }.getOrDefault("This shouldn't occur")
        assert(t == "Something") { "Never-Throw Predefined returned \"$t\"" }
        t = Callable<String> { "This is new" }.getOrDefault(Supplier { "Neither should this" })
        assert(t == "This is new") { "Never-Throw Expensive returned \"$t\"" }
    }
}