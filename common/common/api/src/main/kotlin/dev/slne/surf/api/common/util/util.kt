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
import java.lang.reflect.Field
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*
import java.util.function.BiConsumer
import java.util.function.ToIntFunction
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
 * Modifies a static final object field.
 *
 * @param field The static final field to modify.
 * @param value The new value.
 */
@Deprecated(
    message = "Uses internal Unsafe API which bypasses Java's security and type safety. " +
            "This can cause JVM instability and is not guaranteed to work in future Java versions. " +
            "Consider using VarHandles or redesigning to avoid modifying final fields.",
    level = DeprecationLevel.WARNING
)
fun setStaticFinalField(field: Field, value: Any?) {
    processStaticFinalField(field) { unsafe, fieldBase, fieldOffset ->
        unsafe.putObject(fieldBase, fieldOffset, value)
    }
}

/**
 * Modifies an instance final object field.
 *
 * @param field The final field to modify.
 * @param instance The object containing the field.
 * @param value The new value.
 */
@Deprecated(
    message = "Uses internal Unsafe API which bypasses Java's security and type safety. " +
            "This can cause JVM instability and is not guaranteed to work in future Java versions. " +
            "Consider using VarHandles or redesigning to avoid modifying final fields.",
    level = DeprecationLevel.WARNING
)
fun setFinalField(field: Field, instance: Any, value: Any?) {
    processFinalField(field) { unsafe, fieldOffset ->
        unsafe.putObject(instance, fieldOffset, value)
    }
}

/**
 * Modifies a static final int field.
 *
 * @param field The static final field to modify.
 * @param value The new value.
 */
@Deprecated(
    message = "Uses internal Unsafe API which bypasses Java's security and type safety. " +
            "This can cause JVM instability and is not guaranteed to work in future Java versions. " +
            "Consider using VarHandles or redesigning to avoid modifying final fields.",
    level = DeprecationLevel.WARNING
)
fun setStaticFinalField(field: Field, value: Int) {
    processStaticFinalField(field) { unsafe, fieldBase, fieldOffset ->
        unsafe.putInt(fieldBase, fieldOffset, value)
    }
}

/**
 * Modifies an instance final int field.
 *
 * @param field The final field to modify.
 * @param instance The object containing the field.
 * @param value The new value.
 */
@Deprecated(
    message = "Uses internal Unsafe API which bypasses Java's security and type safety. " +
            "This can cause JVM instability and is not guaranteed to work in future Java versions. " +
            "Consider using VarHandles or redesigning to avoid modifying final fields.",
    level = DeprecationLevel.WARNING
)
fun setFinalField(field: Field, instance: Any, value: Int) {
    processFinalField(field) { unsafe, fieldOffset ->
        unsafe.putInt(instance, fieldOffset, value)
    }
}

/**
 * Modifies a static final long field.
 *
 * @param field The static final field to modify.
 * @param value The new value.
 */
@Deprecated(
    message = "Uses internal Unsafe API which bypasses Java's security and type safety. " +
            "This can cause JVM instability and is not guaranteed to work in future Java versions. " +
            "Consider using VarHandles or redesigning to avoid modifying final fields.",
    level = DeprecationLevel.WARNING
)
fun setStaticFinalField(field: Field, value: Long) {
    processStaticFinalField(field) { unsafe, fieldBase, fieldOffset ->
        unsafe.putLong(fieldBase, fieldOffset, value)
    }
}

/**
 * Modifies an instance final long field.
 *
 * @param field The final field to modify.
 * @param instance The object containing the field.
 * @param value The new value.
 */
@Deprecated(
    message = "Uses internal Unsafe API which bypasses Java's security and type safety. " +
            "This can cause JVM instability and is not guaranteed to work in future Java versions. " +
            "Consider using VarHandles or redesigning to avoid modifying final fields.",
    level = DeprecationLevel.WARNING
)
fun setFinalField(field: Field, instance: Any, value: Long) {
    processFinalField(field) { unsafe, fieldOffset ->
        unsafe.putLong(instance, fieldOffset, value)
    }
}

/**
 * Modifies a static final boolean field.
 *
 * @param field The static final field to modify.
 * @param value The new value.
 */
@Deprecated(
    message = "Uses internal Unsafe API which bypasses Java's security and type safety. " +
            "This can cause JVM instability and is not guaranteed to work in future Java versions. " +
            "Consider using VarHandles or redesigning to avoid modifying final fields.",
    level = DeprecationLevel.WARNING
)
fun setStaticFinalField(field: Field, value: Boolean) {
    processStaticFinalField(field) { unsafe, fieldBase, fieldOffset ->
        unsafe.putBoolean(fieldBase, fieldOffset, value)
    }
}

/**
 * Modifies an instance final boolean field.
 *
 * @param field The final field to modify.
 * @param instance The object containing the field.
 * @param value The new value.
 */
@Deprecated(
    message = "Uses internal Unsafe API which bypasses Java's security and type safety. " +
            "This can cause JVM instability and is not guaranteed to work in future Java versions. " +
            "Consider using VarHandles or redesigning to avoid modifying final fields.",
    level = DeprecationLevel.WARNING
)
fun setFinalField(field: Field, instance: Any, value: Boolean) {
    processFinalField(field) { unsafe, fieldOffset ->
        unsafe.putBoolean(instance, fieldOffset, value)
    }
}

/**
 * Modifies a static final byte field.
 *
 * @param field The static final field to modify.
 * @param value The new value.
 */
@Deprecated(
    message = "Uses internal Unsafe API which bypasses Java's security and type safety. " +
            "This can cause JVM instability and is not guaranteed to work in future Java versions. " +
            "Consider using VarHandles or redesigning to avoid modifying final fields.",
    level = DeprecationLevel.WARNING
)
fun setStaticFinalField(field: Field, value: Byte) {
    processStaticFinalField(field) { unsafe, fieldBase, fieldOffset ->
        unsafe.putByte(fieldBase, fieldOffset, value)
    }
}

/**
 * Modifies an instance final byte field.
 *
 * @param field The final field to modify.
 * @param instance The object containing the field.
 * @param value The new value.
 */
@Deprecated(
    message = "Uses internal Unsafe API which bypasses Java's security and type safety. " +
            "This can cause JVM instability and is not guaranteed to work in future Java versions. " +
            "Consider using VarHandles or redesigning to avoid modifying final fields.",
    level = DeprecationLevel.WARNING
)
fun setFinalField(field: Field, instance: Any, value: Byte) {
    processFinalField(field) { unsafe, fieldOffset ->
        unsafe.putByte(instance, fieldOffset, value)
    }
}

/**
 * Modifies a static final short field.
 *
 * @param field The static final field to modify.
 * @param value The new value.
 */
@Deprecated(
    message = "Uses internal Unsafe API which bypasses Java's security and type safety. " +
            "This can cause JVM instability and is not guaranteed to work in future Java versions. " +
            "Consider using VarHandles or redesigning to avoid modifying final fields.",
    level = DeprecationLevel.WARNING
)
fun setStaticFinalField(field: Field, value: Short) {
    processStaticFinalField(field) { unsafe, fieldBase, fieldOffset ->
        unsafe.putShort(fieldBase, fieldOffset, value)
    }
}

/**
 * Modifies an instance final short field.
 *
 * @param field The final field to modify.
 * @param instance The object containing the field.
 * @param value The new value.
 */
@Deprecated(
    message = "Uses internal Unsafe API which bypasses Java's security and type safety. " +
            "This can cause JVM instability and is not guaranteed to work in future Java versions. " +
            "Consider using VarHandles or redesigning to avoid modifying final fields.",
    level = DeprecationLevel.WARNING
)
fun setFinalField(field: Field, instance: Any, value: Short) {
    processFinalField(field) { unsafe, fieldOffset ->
        unsafe.putShort(instance, fieldOffset, value)
    }
}

/**
 * Modifies a static final float field.
 *
 * @param field The static final field to modify.
 * @param value The new value.
 */
@Deprecated(
    message = "Uses internal Unsafe API which bypasses Java's security and type safety. " +
            "This can cause JVM instability and is not guaranteed to work in future Java versions. " +
            "Consider using VarHandles or redesigning to avoid modifying final fields.",
    level = DeprecationLevel.WARNING
)
fun setStaticFinalField(field: Field, value: Float) {
    processStaticFinalField(field) { unsafe, fieldBase, fieldOffset ->
        unsafe.putFloat(fieldBase, fieldOffset, value)
    }
}

/**
 * Modifies an instance final float field.
 *
 * @param field The final field to modify.
 * @param instance The object containing the field.
 * @param value The new value.
 */
@Deprecated(
    message = "Uses internal Unsafe API which bypasses Java's security and type safety. " +
            "This can cause JVM instability and is not guaranteed to work in future Java versions. " +
            "Consider using VarHandles or redesigning to avoid modifying final fields.",
    level = DeprecationLevel.WARNING
)
fun setFinalField(field: Field, instance: Any, value: Float) {
    processFinalField(field) { unsafe, fieldOffset ->
        unsafe.putFloat(instance, fieldOffset, value)
    }
}

/**
 * Modifies a static final double field.
 *
 * @param field The static final field to modify.
 * @param value The new value.
 */
@Deprecated(
    message = "Uses internal Unsafe API which bypasses Java's security and type safety. " +
            "This can cause JVM instability and is not guaranteed to work in future Java versions. " +
            "Consider using VarHandles or redesigning to avoid modifying final fields.",
    level = DeprecationLevel.WARNING
)
fun setStaticFinalField(field: Field, value: Double) {
    processStaticFinalField(field) { unsafe, fieldBase, fieldOffset ->
        unsafe.putDouble(fieldBase, fieldOffset, value)
    }
}

/**
 * Modifies an instance final double field.
 *
 * @param field The final field to modify.
 * @param instance The object containing the field.
 * @param value The new value.
 */
@Deprecated(
    message = "Uses internal Unsafe API which bypasses Java's security and type safety. " +
            "This can cause JVM instability and is not guaranteed to work in future Java versions. " +
            "Consider using VarHandles or redesigning to avoid modifying final fields.",
    level = DeprecationLevel.WARNING
)
fun setFinalField(field: Field, instance: Any, value: Double) {
    processFinalField(field) { unsafe, fieldOffset ->
        unsafe.putDouble(instance, fieldOffset, value)
    }
}

/**
 * Modifies a static final char field.
 *
 * @param field The static final field to modify.
 * @param value The new value.
 */
@Deprecated(
    message = "Uses internal Unsafe API which bypasses Java's security and type safety. " +
            "This can cause JVM instability and is not guaranteed to work in future Java versions. " +
            "Consider using VarHandles or redesigning to avoid modifying final fields.",
    level = DeprecationLevel.WARNING
)
fun setStaticFinalField(field: Field, value: Char) {
    processStaticFinalField(field) { unsafe, fieldBase, fieldOffset ->
        unsafe.putChar(fieldBase, fieldOffset, value)
    }
}

/**
 * Modifies an instance final char field.
 *
 * @param field The final field to modify.
 * @param instance The object containing the field.
 * @param value The new value.
 */
@Deprecated(
    message = "Uses internal Unsafe API which bypasses Java's security and type safety. " +
            "This can cause JVM instability and is not guaranteed to work in future Java versions. " +
            "Consider using VarHandles or redesigning to avoid modifying final fields.",
    level = DeprecationLevel.WARNING
)
fun setFinalField(field: Field, instance: Any, value: Char) {
    processFinalField(field) { unsafe, fieldOffset ->
        unsafe.putChar(instance, fieldOffset, value)
    }
}

/**
 * Internal helper for modifying static final fields using Unsafe.
 */
private fun processStaticFinalField(field: Field, putOperation: (Unsafe, Any, Long) -> Unit) {
    val fieldBase = unsafe.staticFieldBase(field)
    val fieldOffset = unsafe.staticFieldOffset(field)
    putOperation(unsafe, fieldBase, fieldOffset)
}

/**
 * Internal helper for modifying instance final fields using Unsafe.
 */
private fun processFinalField(field: Field, putOperation: (Unsafe, Long) -> Unit) {
    val fieldOffset = unsafe.objectFieldOffset(field)
    putOperation(unsafe, fieldOffset)
}

/**
 * Creates a read-only map from enum constants to their string IDs.
 *
 * @param enumClass The enum class.
 * @param idMapper Function mapping each enum constant to its string ID.
 * @return Read-only map from string IDs to enum constants.
 */
fun <T : Enum<T>> byStringIdMap(
    enumClass: Class<T>,
    idMapper: (T) -> String,
): Map<String, T> = enumClass.enumConstants.associateBy(idMapper)

/**
 * Creates a read-only map from enum constants to their integer IDs.
 *
 * @param enumClass The enum class.
 * @param idMapper Function mapping each enum constant to its integer ID.
 * @return Read-only map from integer IDs to enum constants.
 */
fun <T : Enum<T>> byIdMap(
    enumClass: Class<T>,
    idMapper: ToIntFunction<T>,
): Map<Int, T> {
    return byIdMap(idMapper, enumClass.enumConstants)
}

/**
 * Creates a read-only map from values to their integer IDs.
 *
 * @param idMapper Function mapping each value to its integer ID.
 * @param values Array of values to map.
 * @return Read-only map from integer IDs to values.
 */
fun <T> byIdMap(
    idMapper: ToIntFunction<T>,
    values: Array<T>,
): Map<Int, T> {
    return values.associateBy { idMapper.applyAsInt(it) }
}

/**
 * Creates a read-only map from values to their integer IDs.
 *
 * @param idMapper Function mapping each value to its integer ID.
 * @param values Array of values to map.
 * @return Read-only map from integer IDs to values.
 */
fun <T> byIdMap(
    idMapper: (T) -> Int,
    values: Array<T>,
): Map<Int, T> {
    return values.associateBy(idMapper)
}

/**
 * Creates a read-only map from values to their byte IDs.
 *
 * @param values Array of values to map.
 * @param idMapper Function mapping each value to its byte ID.
 * @return Read-only map from byte IDs to values.
 */
fun <T> byByteIdMap(
    values: Array<T>,
    idMapper: (T) -> Byte,
): Map<Byte, T> {
    return values.associateBy(idMapper)
}

/**
 * Creates a read-only map from enum constants to computed values.
 *
 * @param valueMapper Function mapping each enum constant to its associated value.
 * @return Read-only map from enum constants to their values.
 */
inline fun <reified T : Enum<T>> byEnumMap(
    valueMapper: (T) -> Any,
): Map<T, Any> {
    return T::class.java.enumConstants.associateWith(valueMapper)
}

/**
 * Functional interface for mapping a value to a byte.
 */
fun interface ToByteFunction<T> {
    fun applyAsByte(value: T): Byte
}

/**
 * Interface for types that can be identified by an enum value.
 */
fun interface ByEnum<T> {
    fun value(): T
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