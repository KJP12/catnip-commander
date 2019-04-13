package net.kjp12.commands.utils

import junit.framework.AssertionFailedError
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.util.concurrent.Callable
import java.util.function.Supplier

class MiscellaneousUtilsTest {
    @Test
    fun getOrDefault() {
        var t = Callable<String> { throw Exception("This should be swallowed") }.getOrDefault("Something was thrown")
        assert(t == "Something was thrown") { "Always-Throw Predefined returned \"$t\"" }
        t =
            Callable<String> { throw Exception("This should be swallowed") }.getOrDefault(Supplier { "Something was definitely thrown; I'm an auto-generated stub." })
        assert(t == "Something was definitely thrown; I'm an auto-generated stub.") { "Always-Throw Expensive returned \"$t\"" }
        try {
            t = Callable<String> { throw Exception("This should be re-tossed as an Error") }.getOrThrow(DurianConsumer {
                throw Error(
                    "Something Fatal",
                    it
                )
            })
            fail("Failed to throw \"Something Fatal\" error, got \"$t\"")
        } catch (afe: AssertionFailedError) {
            throw afe
        } catch (e: Error) {
            if (e.message != "Something Fatal") throw AssertionError("Expected \"Something Failed\"", e)
            if (e.cause == e || e.cause == null) throw AssertionError("Expected cause", e)
            if (e.cause!!.message != "This should be re-tossed as an Error") throw AssertionError(
                "Expected Exception \"This should be re-tossed as an Error\"",
                e
            )
        }

        t = Callable<String> { "Something" }.getOrDefault("This shouldn't occur")
        assert(t == "Something") { "Never-Throw Predefined returned \"$t\"" }
        t = Callable<String> { "This is new" }.getOrDefault(Supplier { "Neither should this" })
        assert(t == "This is new") { "Never-Throw Expensive returned \"$t\"" }
        t = Callable<String> { "Exceptionally" }.getOrThrow(DurianConsumer {
            throw AssertionError(
                "This shouldn't occur",
                it
            )
        })
        assert(t == "Exceptionally") { "Never-Throw Throw returned \"$t\"" }
    }
}