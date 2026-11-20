@file:OptIn(ExperimentalContracts::class)
@file:JvmName("SurfUtil")
@file:Suppress("removal", "DEPRECATION")

package dev.slne.surf.api.common.util

import com.google.common.flogger.FluentLogger
import com.google.common.flogger.LoggingApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import sun.misc.Unsafe
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*
import java.util.function.BiConsumer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KProperty
import kotlin.streams.asSequence

private fun createRandom(): SecureRandom = try {
    SecureRandom.getInstance("DRBG")
} catch (_: NoSuchAlgorithmException) {
    SecureRandom()
}

private val platformRandom = ThreadLocal.withInitial(::createRandom)

private object VirtualRandomPool {
    val instances = Array(32) { createRandom() }
}

/**
 * Returns a secure random generator for the current thread.
 *
 * It tries to use the non-blocking `DRBG` algorithm and falls back to the default
 * `SecureRandom` if unavailable.
 *
 * Platform threads use thread-local instances, while virtual threads
 * share a fixed pool to avoid excessive generator allocations.
 */
val random: SecureRandom
    get() {
        val thread = Thread.currentThread()

        return if (thread.isVirtual) {
            val instances = VirtualRandomPool.instances
            val index = (thread.threadId() and (instances.size - 1).toLong()).toInt()
            instances[index]
        } else {
            platformRandom.get()
        }
    }

/**
 * Returns a [FluentLogger] for the enclosing class.
 *
 * This function is caller-sensitive and must be inlined to correctly identify the enclosing class.
 */
@Suppress("NOTHING_TO_INLINE") // Caller sensitive
inline fun logger(): FluentLogger = FluentLogger.forEnclosingClass()

/**
 * Executes the logging operation only if the condition evaluates to true.
 *
 * @param condition Lambda returning true if logging should occur.
 * @param logOperation Logging operation to execute conditionally.
 */
inline fun <API : LoggingApi<API>> LoggingApi<API>.logIf(
    condition: () -> Boolean,
    logOperation: LoggingApi<API>.() -> Unit,
) {
    contract {
        callsInPlace(logOperation, InvocationKind.AT_MOST_ONCE)
    }

    if (condition()) {
        logOperation()
    }
}

/**
 * Stack walker instance configured to retain class references for caller inspection.
 */
private val STACK_WALK_INSTANCE: StackWalker =
    StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)

/**
 * Returns the class of the immediate caller.
 *
 * @return The caller's class, or null if unavailable.
 */
fun getCallerClass() = getCallerClass(0)

/**
 * Returns the class of the caller at the specified stack depth.
 *
 * @param depth Number of additional stack frames to skip (0 for immediate caller).
 * @return The caller's class at the specified depth, or null if unavailable.
 */
fun getCallerClass(depth: Int) =
    STACK_WALK_INSTANCE.walk { it.asSequence().drop(3 + depth).firstOrNull()?.declaringClass }

/**
 * Verifies that the immediate caller is the expected class.
 *
 * @param expected The expected caller class.
 * @throws IllegalStateException if the caller does not match.
 */
fun checkCallerClass(expected: Class<*>) {
    if (getCallerClass(1) != expected) {
        throw IllegalStateException("Caller class is not $expected")
    }
}

/**
 * Direct access to the JVM's Unsafe API.
 *
 * This provides low-level memory operations that bypass Java's type safety and access control.
 * Use only when absolutely necessary, as misuse can cause JVM crashes.
 */
val unsafe = try {
    val unsafeField = Unsafe::class.java.getDeclaredField("theUnsafe")
    unsafeField.isAccessible = true
    unsafeField.get(null) as Unsafe
} catch (e: Exception) {
    throw RuntimeException(e)
}

/**
 * Converts a sequence to an enumeration.
 *
 * @return An enumeration that iterates over the sequence elements.
 */
fun <T> Sequence<T>.toEnumeration(): Enumeration<T> {
    val iterator = iterator()
    return object : Enumeration<T> {
        override fun hasMoreElements(): Boolean = iterator.hasNext()
        override fun nextElement(): T = iterator.next()
    }
}

/**
 * Returns the size of this iterable if it is a collection, otherwise returns the default value.
 *
 * @param default The value to return if this is not a collection.
 * @return The collection size, or the default value.
 */
fun <E> Iterable<E>.collectionSizeOrDefault(default: Int) =
    if (this is Collection<*>) this.size else default

/**
 * Delegation operator for [WeakReference] allowing property-style access.
 *
 * @return The referenced object, or null if collected.
 */
operator fun <T> WeakReference<T>.getValue(thisRef: Any?, property: KProperty<*>): T? {
    return get()
}

/**
 * Delegation operator for [SoftReference] allowing property-style access.
 *
 * @return The referenced object, or null if collected.
 */
operator fun <T> SoftReference<T>.getValue(thisRef: Any?, property: KProperty<*>): T? {
    return get()
}

/**
 * Maps each element asynchronously using coroutines.
 *
 * All transformations run concurrently, and this function suspends until all complete.
 *
 * @param transform Suspending transformation function applied to each element.
 * @return List of transformed elements in the original order.
 */
suspend inline fun <E, R> Iterable<E>.mapAsync(crossinline transform: suspend (E) -> R): List<R> {
    return coroutineScope {
        map { element ->
            async {
                transform(element)
            }
        }.awaitAll()
    }
}

/**
 * Returns a new [BiConsumer] that first executes the given [prepend] consumer before this one.
 *
 * @return A composed [BiConsumer] that performs the [prepend] operation followed by this operation.
 */
fun <T, U> BiConsumer<T, U>?.prepend(prepend: BiConsumer<in T, in U>): BiConsumer<T, U> = { t, u ->
    prepend.accept(t, u)
    this?.accept(t, u)
}