package net.kjp12.commands.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.Callable
import java.util.function.Supplier

class MiscellaneousUtilsTest {

    @Test fun getOrDefaultStringThrowingPredefined() = assertEquals("This is a predefined string.", Callable<String> { throw Exception("This should be swallowed.") }.getOrDefault("This is a predefined string."))

    @Test fun getOrDefaultStringThrowingSupplied() = assertEquals("This is a supplied string.", Callable<String> { throw Exception("This should be swallowed.") }.getOrDefault(Supplier { "This is a supplied string." }))

    //Due to the way Kotlin does lambda, this is impossible to test.
    //@Test fun getOrDefaultStringThrowingSuppliedKotlinLambda() = assertEquals("This is a supplied string.", Callable<String> { throw Exception("This should be swallowed.") }.getOrDefault { -> "This is a supplied string." })

    @Test fun getOrDefaultStringReturningPredefined() = assertEquals("Predefined suppressed.", Callable { "Predefined suppressed." }.getOrDefault("This is a predefined string."))

    @Test fun getOrDefaultStringReturningSupplied() = assertEquals("Supplier suppressed.", Callable { "Supplier suppressed." }.getOrDefault(Supplier { "This is a supplied string." }))

    //Due to the way Kotlin does lambda, this is impossible to test.
    //@Test fun getOrDefaultStringReturningSuppliedKotlinLambda() = assertEquals("Supplier suppressed.", Callable { "Supplier suppressed." }.getOrDefault { "This is a supplied string." })

    val obj1 = Any()
    val obj2 = Any()

    init {
        assertNotEquals(obj1, obj2, "Initialization error")
    }

    @Test fun getOrDefaultObjectThrowingPredefined() = assertEquals(obj1, Callable<Any> { throw Exception("This should be swallowed.") }.getOrDefault(obj1))

    //Due to ambiguous methods on Any, `supplier =` has to be used.
    @Test fun getOrDefaultObjectThrowingSupplied() = assertEquals(obj1, Callable<Any> { throw Exception("This should be swallowed.") }.getOrDefault(supplier = Supplier { obj1 }))

    //Due to the way Kotlin does lambda, this is impossible to test.
    //@Test fun getOrDefaultObjectThrowingSuppliedKotlinLambda() = assertEquals(obj1, Callable<Any> { throw Exception("This should be swallowed.") }.getOrDefault(supplier = { obj1 }))

    @Test fun getOrDefaultObjectReturningPredefined() = assertEquals(obj2, Callable { obj2 }.getOrDefault(obj1))

    //Due to ambiguous methods on Any, `supplier =` has to be used.
    @Test fun getOrDefaultObjectReturningSupplied() = assertEquals(obj2, Callable { obj2 }.getOrDefault(supplier = Supplier { obj1 }))

    //Due to the way Kotlin does lambda, this is impossible to test.
    //@Test fun getOrDefaultObjectReturningSuppliedKotlinLambda() = assertEquals(obj2, Callable { obj2 }.getOrDefault(supplier = { obj1 }))
}