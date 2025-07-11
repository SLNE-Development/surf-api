@file:OptIn(ExperimentalContracts::class)
@file:JvmName("SurfUtil")

package dev.slne.surf.surfapi.core.api.util

import com.google.common.flogger.FluentLogger
import com.google.common.flogger.LoggingApi
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMaps
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import sun.misc.Unsafe
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.security.SecureRandom
import java.util.*
import java.util.function.ToIntFunction
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KProperty
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

val unsafe = try {
    val unsafeField = Unsafe::class.java.getDeclaredField("theUnsafe")
    unsafeField.isAccessible = true
    unsafeField.get(null) as Unsafe
} catch (e: Exception) {
    throw RuntimeException(e)
}

fun setStaticFinalField(field: Field, value: Any?) {
    processStaticFinalField(field) { unsafe, fieldBase, fieldOffset ->
        unsafe.putObject(fieldBase, fieldOffset, value)
    }
}

fun setFinalField(field: Field, instance: Any, value: Any?) {
    processFinalField(field) { unsafe, fieldOffset ->
        unsafe.putObject(instance, fieldOffset, value)
    }
}

fun setStaticFinalField(field: Field, value: Int) {
    processStaticFinalField(field) { unsafe, fieldBase, fieldOffset ->
        unsafe.putInt(fieldBase, fieldOffset, value)
    }
}

fun setFinalField(field: Field, instance: Any, value: Int) {
    processFinalField(field) { unsafe, fieldOffset ->
        unsafe.putInt(instance, fieldOffset, value)
    }
}

fun setStaticFinalField(field: Field, value: Long) {
    processStaticFinalField(field) { unsafe, fieldBase, fieldOffset ->
        unsafe.putLong(fieldBase, fieldOffset, value)
    }
}

fun setFinalField(field: Field, instance: Any, value: Long) {
    processFinalField(field) { unsafe, fieldOffset ->
        unsafe.putLong(instance, fieldOffset, value)
    }
}

fun setStaticFinalField(field: Field, value: Boolean) {
    processStaticFinalField(field) { unsafe, fieldBase, fieldOffset ->
        unsafe.putBoolean(fieldBase, fieldOffset, value)
    }
}

fun setFinalField(field: Field, instance: Any, value: Boolean) {
    processFinalField(field) { unsafe, fieldOffset ->
        unsafe.putBoolean(instance, fieldOffset, value)
    }
}

fun setStaticFinalField(field: Field, value: Byte) {
    processStaticFinalField(field) { unsafe, fieldBase, fieldOffset ->
        unsafe.putByte(fieldBase, fieldOffset, value)
    }
}

fun setFinalField(field: Field, instance: Any, value: Byte) {
    processFinalField(field) { unsafe, fieldOffset ->
        unsafe.putByte(instance, fieldOffset, value)
    }
}

fun setStaticFinalField(field: Field, value: Short) {
    processStaticFinalField(field) { unsafe, fieldBase, fieldOffset ->
        unsafe.putShort(fieldBase, fieldOffset, value)
    }
}

fun setFinalField(field: Field, instance: Any, value: Short) {
    processFinalField(field) { unsafe, fieldOffset ->
        unsafe.putShort(instance, fieldOffset, value)
    }
}

fun setStaticFinalField(field: Field, value: Float) {
    processStaticFinalField(field) { unsafe, fieldBase, fieldOffset ->
        unsafe.putFloat(fieldBase, fieldOffset, value)
    }
}

fun setFinalField(field: Field, instance: Any, value: Float) {
    processFinalField(field) { unsafe, fieldOffset ->
        unsafe.putFloat(instance, fieldOffset, value)
    }
}

fun setStaticFinalField(field: Field, value: Double) {
    processStaticFinalField(field) { unsafe, fieldBase, fieldOffset ->
        unsafe.putDouble(fieldBase, fieldOffset, value)
    }
}

fun setFinalField(field: Field, instance: Any, value: Double) {
    processFinalField(field) { unsafe, fieldOffset ->
        unsafe.putDouble(instance, fieldOffset, value)
    }
}

fun setStaticFinalField(field: Field, value: Char) {
    processStaticFinalField(field) { unsafe, fieldBase, fieldOffset ->
        unsafe.putChar(fieldBase, fieldOffset, value)
    }
}

fun setFinalField(field: Field, instance: Any, value: Char) {
    processFinalField(field) { unsafe, fieldOffset ->
        unsafe.putChar(instance, fieldOffset, value)
    }
}

private fun processStaticFinalField(field: Field, putOperation: (Unsafe, Any, Long) -> Unit) {
    val fieldBase = unsafe.staticFieldBase(field)
    val fieldOffset = unsafe.staticFieldOffset(field)
    putOperation(unsafe, fieldBase, fieldOffset)
}

private fun processFinalField(field: Field, putOperation: (Unsafe, Long) -> Unit) {
    val fieldOffset = unsafe.objectFieldOffset(field)
    putOperation(unsafe, fieldOffset)
}

fun <T : Enum<T>> byStringIdMap(
    enumClass: Class<T>,
    idMapper: (T) -> String,
): Object2ObjectMap<String, T> = Object2ObjectMaps.unmodifiable(
    Object2ObjectOpenHashMap(
        enumClass.enumConstants.associateBy(idMapper)
    )
)


fun <T : Enum<T>> byIdMap(
    enumClass: Class<T>,
    idMapper: ToIntFunction<T>,
): Int2ObjectMap<T> {
    return byIdMap(idMapper, enumClass.enumConstants)
}

fun <T> byIdMap(
    idMapper: ToIntFunction<T>,
    values: Array<T>,
): Int2ObjectMap<T> {
    return Int2ObjectMaps.unmodifiable(
        Int2ObjectOpenHashMap(
            values.associateBy({ idMapper.applyAsInt(it) }, { it })
        )
    )
}

fun <T> byIdMap(
    idMapper: (T) -> Int,
    values: Array<T>,
): Int2ObjectMap<T> {
    return Int2ObjectMaps.unmodifiable(
        Int2ObjectOpenHashMap(
            values.associateBy(idMapper)
        )
    )
}

fun <T> byByteIdMap(
    values: Array<T>,
    idMapper: (T) -> Byte,
): Byte2ObjectMap<T> {
    return Byte2ObjectMaps.unmodifiable(
        Byte2ObjectOpenHashMap(
            values.associateBy(idMapper)
        )
    )
}

inline fun <reified T : Enum<T>> byEnumMap(
    valueMapper: (T) -> Any,
): Object2ObjectMap<T, Any> {
    return Object2ObjectMaps.unmodifiable(
        Object2ObjectOpenHashMap(
            T::class.java.enumConstants.associateWith(valueMapper)
        )
    )
}

fun interface ToByteFunction<T> {
    fun applyAsByte(value: T): Byte
}

fun interface ByEnum<T> {
    fun value(): T
}

fun <T> Sequence<T>.toEnumeration(): Enumeration<T> {
    val iterator = iterator()
    return object : Enumeration<T> {
        override fun hasMoreElements(): Boolean = iterator.hasNext()
        override fun nextElement(): T = iterator.next()
    }
}

fun <E> Iterable<E>.collectionSizeOrDefault(default: Int) =
    if (this is Collection<*>) this.size else default

operator fun <T> WeakReference<T>.getValue(thisRef: Any?, property: KProperty<*>): T? {
    return get()
}

operator fun <T> SoftReference<T>.getValue(thisRef: Any?, property: KProperty<*>): T? {
    return get()
}