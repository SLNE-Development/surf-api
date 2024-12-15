@file:OptIn(ExperimentalContracts::class)

package dev.slne.surf.surfapi.core.api.util

import com.google.common.flogger.FluentLogger
import com.google.common.flogger.LoggingApi
import java.security.SecureRandom
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.streams.asSequence

/**
 * A lazily-initialized instance of `SecureRandom` providing a secure source of randomness.
 *
 * This variable attempts to obtain a strong `SecureRandom` instance using `SecureRandom.getInstanceStrong()`.
 * If this operation fails (e.g., due to system or environment limitations), it logs the error and falls back
 * to a default `SecureRandom` instance.
 *
 * This ensures that a `SecureRandom` instance is always available, prioritizing strong cryptographic security
 * when possible, while maintaining resilience to initialization failures.
 */
val random: SecureRandom by lazy {
    try {
        SecureRandom.getInstanceStrong()
    } catch (e: Exception) {
        logger().atWarning()
            .withCause(e)
            .log("Failed to get strong SecureRandom, falling back to default")
        SecureRandom()
    }
}

/**
 * Provides a `FluentLogger` instance associated with the enclosing class.
 *
 * This method is intended to simplify access to `FluentLogger` for logging
 * purposes, automatically associating the logger with the class that calls it.
 *
 * @return A `FluentLogger` instance for the enclosing class.
 */
@Suppress("NOTHING_TO_INLINE") // Caller sensitive
inline fun logger(): FluentLogger = FluentLogger.forEnclosingClass()

/**
 * Executes the provided logging operation if the specified condition evaluates to true.
 *
 * This function allows for conditional logging using a specified `LoggingApi`.
 * The logging operation will only be performed if the `condition` lambda returns true.
 *
 * @param condition A lambda that provides a condition to evaluate. The logging operation will be executed only if this condition returns true.
 * @param logOperation The logging operation to perform if the condition is satisfied. This is an extension function on the logging API.
 */
inline fun <API : LoggingApi<API>> LoggingApi<API>.logIf(
    condition: () -> Boolean,
    logOperation: LoggingApi<API>.() -> Unit
) {
    contract {
        callsInPlace(logOperation, InvocationKind.AT_MOST_ONCE)
    }

    if (condition()) {
        logOperation()
    }
}

/**
 * A singleton instance of `StackWalker` configured with the `Option.RETAIN_CLASS_REFERENCE` option.
 *
 * This instance is used for retrieving information about the current call stack,
 * including details such as the declaring class of a caller. The
 * `RETAIN_CLASS_REFERENCE` option ensures that the `Class` objects of stack frames
 * are retained, making it possible to perform operations requiring class meta-information.
 *
 * Typical usage of this instance involves walking the stack to identify or introspect
 * the caller classes or methods with precise class retention capabilities. This is
 * particularly beneficial in scenarios where well-defined caller verification or context
 * inference is needed.
 */
private val STACK_WALK_INSTANCE: StackWalker =
    StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)

/**
 * Retrieves the class of the method's immediate caller.
 *
 * This utility function uses a `StackWalker` instance to walk the call stack
 * and retrieve the class that invoked the current method.
 * The `depth` parameter determines how far back in the call stack the search
 * proceeds, relative to the default offset used internally.
 *
 * This function is useful for tasks such as debugging, logging, or implementing
 * caller-sensitive behaviors.
 *
 * Note: Invoking this function may have performance overhead depending on the
 * depth of the stack and the number of elements traversed.
 *
 * @return The `Class` object of the caller, or null if no caller is found in the
 * stack at the specified depth.
 */
fun getCallerClass() = getCallerClass(0)

/**
 * Retrieves the class of a caller at a specific depth in the call stack.
 *
 * This utility function uses a `StackWalker` instance to walk the call stack
 * and retrieve the class that invoked the current method at a specified depth.
 * The `depth` parameter determines how many levels to move up in the stack
 * to locate the desired caller.
 *
 * This function is commonly used for tasks such as debugging, logging, or implementing
 * caller-sensitive behaviors.
 *
 * Note: The default offset of 3 accounts for the internal implementation of the stack walker.
 *
 * @param depth The number of levels to walk up the call stack to locate the desired caller.
 *              A value of 0 corresponds to the immediate caller, while higher values
 *              move further up the stack.
 * @return The `Class` object of the caller at the specified depth, or null if no class
 *         is found at that depth in the call stack.
 */
fun getCallerClass(depth: Int) =
    STACK_WALK_INSTANCE.walk { it.asSequence().drop(3 + depth).firstOrNull()?.declaringClass }

/**
 * Checks if the immediate caller class matches the expected class.
 *
 * This function verifies that the class of the immediate caller matches the provided
 * expected class. If the caller class does not match, an `IllegalStateException` is thrown.
 *
 * @param expected The `Class` object representing the expected caller class. This is the class
 *                 that the function expects to be the direct invoker of the current method.
 * @throws IllegalStateException If the class of the immediate caller does not match the expected class.
 */
fun checkCallerClass(expected: Class<*>) {
    if (getCallerClass(1) != expected) {
        throw IllegalStateException("Caller class is not $expected")
    }
}

/**
 * Verifies that the current method is instantiated by `java.util.ServiceLoader`.
 *
 * This function checks the call stack to ensure that the class instantiating
 * an object is `java.util.ServiceLoader`. It prevents direct instantiation of
 * the object outside of the expected context (i.e., via `ServiceLoader`).
 *
 * If the instantiation is not performed by `ServiceLoader`, an `IllegalStateException`
 * is thrown with an appropriate error message, enforcing proper usage.
 *
 * @throws IllegalStateException If the instantiation is not done by `java.util.ServiceLoader`.
 */
fun checkInstantiationByServiceLoader() {
    check(getCallerClass(1)?.name?.startsWith("java.util.ServiceLoader") == true) { "Cannot instantiate instance directly" }
}