package dev.slne.surf.api.core.util

import dev.slne.surf.api.core.exception.CompoundRuntimeException
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.slf4j.Logger
import java.util.*

/**
 * Removes [value] from the mutable list stored under [key].
 *
 * If the list becomes empty after removal, the [key] entry is removed from this map.
 * If no list exists for [key], this function does nothing.
 *
 * **Thread safety:** This function is not synchronized. Concurrent safety depends on the
 * receiver map and the contained mutable lists. Use only with external synchronization or
 * thread-safe map/list implementations when the map may be accessed concurrently.
 *
 * @param key the key whose associated list should be updated.
 * @param value the value to remove from the associated list.
 */
fun <K, V> MutableMap<K, MutableList<V>>.remove(key: K, value: V) {
    compute(key) { _, list ->
        if (list != null) {
            list.remove(value)
            if (list.isEmpty()) null else list
        } else null
    }
}

/**
 * Removes [value] from the mutable list stored under [key].
 *
 * If the list becomes empty after removal, the [key] entry is removed from this map.
 * If no list exists for [key], this function does nothing.
 *
 * **Thread safety:** This function is not synchronized. Concurrent safety depends on the
 * receiver map and the contained mutable lists. Use only with external synchronization or
 * thread-safe map/list implementations when the map may be accessed concurrently.
 *
 * @param key the key whose associated list should be updated.
 * @param value the value to remove from the associated list.
 */
fun <K : Any, V> Map<K, V>.without(key: K): Map<K, V> {
    if (!containsKey(key)) {
        return this
    } else if (size == 1) {
        return emptyMap()
    } else {
        val result = Object2ObjectOpenHashMap<K, V>(size - 1, 0.5f)
        result.putAll(this)
        for ((k, v) in this) {
            if (k != key) {
                result[k] = v
            }
        }
        return result
    }
}

/**
 * Removes [value] from the mutable list stored under [key].
 *
 * If the list becomes empty after removal, the [key] entry is removed from this map.
 * If no list exists for [key], this function does nothing.
 *
 * **Thread safety:** This function is not synchronized. Concurrent safety depends on the
 * receiver map and the contained mutable lists. Use only with external synchronization or
 * thread-safe map/list implementations when the map may be accessed concurrently.
 *
 * @param key the key whose associated list should be updated.
 * @param value the value to remove from the associated list.
 */
fun <K : Any, V> Map<K, V>.with(key: K, value: V): Map<K, V> {
    val size = size
    if (size == 0) {
        return Object2ObjectMaps.singleton(key, value)
    }

    val result = Object2ObjectOpenHashMap<K, V>(size + 1, 0.5f)
    result.putAll(this)
    result[key] = value
    return result
}

/**
 * Returns a map containing all entries from this map and [otherMap].
 *
 * Entries from [otherMap] override entries from this map when both maps contain the same key.
 *
 * **Thread safety:** This function is not synchronized. It is safe for concurrent reads only if
 * neither source map is being mutated while the result is being created. The returned map is
 * mutable internally but exposed as [Map], and is not safe for concurrent mutation without
 * external synchronization.
 *
 * @param otherMap the map whose entries should be added to this map's entries.
 * @return a map containing entries from both maps.
 */
fun <K : Any, V> Map<K, V>.withAll(otherMap: Map<K, V>): Map<K, V> {
    val totalSize = size + otherMap.size
    if (totalSize == 0) {
        return mapOf()
    }

    val result = Object2ObjectOpenHashMap<K, V>(totalSize, 0.5f)
    result.putAll(this)
    result.putAll(otherMap)
    return result
}

/**
 * Adds [value] to the mutable list stored under [key].
 *
 * If no list exists for [key], a new list is created and inserted.
 *
 * **Thread safety:** This function is not synchronized. Concurrent safety depends on the
 * receiver map and the contained mutable lists. Use only with external synchronization or
 * thread-safe map/list implementations when the map may be accessed concurrently.
 *
 * @param key the key whose associated list should receive [value].
 * @param value the value to add to the associated list.
 */
fun <K, V> MutableMap<K, MutableList<V>>.putValue(key: K, value: V) {
    compute(key) { _, list ->
        if (list == null) {
            ObjectArrayList<V>().apply { add(value) }
        } else {
            list.add(value)
            list
        }
    }
}

/**
 * Returns all elements except the first element.
 *
 * The returned list is a view backed by this list.
 *
 * **Thread safety:** This function is not synchronized. The returned view is safe for concurrent
 * reads only if the backing list is not mutated concurrently.
 *
 * @return a view containing all elements after the first.
 * @throws IllegalArgumentException if this list is empty.
 */
fun <T> List<T>.tail(): List<T> {
    require(isNotEmpty())
    return subList(1, size)
}

/**
 * Returns all elements except the first element.
 *
 * The returned list is a view backed by this list.
 *
 * **Thread safety:** This function is not synchronized. The returned view is safe for concurrent
 * reads only if the backing list is not mutated concurrently.
 *
 * @return a view containing all elements after the first.
 * @throws IllegalArgumentException if this list is empty.
 */
fun <T> List<T>.tailOrEmpty(): List<T> {
    if (isEmpty()) return emptyList()
    return subList(1, size)
}

/**
 * Returns the first element together with the remaining elements.
 *
 * The tail list is a view backed by this list.
 *
 * **Thread safety:** This function is not synchronized. The returned tail view is safe for
 * concurrent reads only if the backing list is not mutated concurrently.
 *
 * @return a pair containing the first element and the tail of this list.
 * @throws NoSuchElementException if this list is empty.
 * @throws IllegalArgumentException if this list is empty.
 */
fun <T> List<T>.headTail(): Pair<T, List<T>> = Pair(first(), tail())

/**
 * Returns the first element together with the remaining elements.
 *
 * The tail list is a view backed by this list.
 *
 * **Thread safety:** This function is not synchronized. The returned tail view is safe for
 * concurrent reads only if the backing list is not mutated concurrently.
 *
 * @return a pair containing the first element and the tail of this list.
 * @throws NoSuchElementException if this list is empty.
 * @throws IllegalArgumentException if this list is empty.
 */
fun <T> List<T>.headTailOrNull(): Pair<T, List<T>>? = if (isEmpty()) null else headTail()

/**
 * Returns all elements except the last element.
 *
 * The returned list is a view backed by this list.
 *
 * **Thread safety:** This function is not synchronized. The returned view is safe for concurrent
 * reads only if the backing list is not mutated concurrently.
 *
 * @return a view containing all elements before the last.
 * @throws IllegalArgumentException if this list is empty.
 */
fun <T> List<T>.init(): List<T> {
    require(isNotEmpty())
    return subList(0, size - 1)
}

/**
 * Returns `null` if this list is `null` or empty; otherwise returns this list.
 *
 * **Thread safety:** This function is not synchronized. The returned list is the original list,
 * so concurrent safety depends on the receiver's implementation and usage.
 *
 * @return this list when it contains at least one element, or `null`.
 */
fun <T> List<T>?.nullize(): List<T>? {
    return if (this.isNullOrEmpty()) null else this
}

/**
 * Returns `null` if this list is `null` or empty; otherwise returns this list.
 *
 * **Thread safety:** This function is not synchronized. The returned list is the original list,
 * so concurrent safety depends on the receiver's implementation and usage.
 *
 * @return this list when it contains at least one element, or `null`.
 */
fun <T> Array<T>?.nullize(): Array<T>? {
    return if (this.isNullOrEmpty()) null else this
}

/**
 * Applies [operation] to every element of this array and guarantees that all elements are visited.
 *
 * Exceptions thrown by [operation] are collected and rethrown after iteration as a
 * [CompoundRuntimeException].
 *
 * **Thread safety:** This function is not synchronized. It is safe for concurrent reads only if
 * this array is not mutated concurrently and [operation] is safe for concurrent use.
 *
 * @param operation the operation to apply to each element.
 * @throws CompoundRuntimeException if one or more invocations of [operation] throw.
 */
inline fun <T> Array<out T>.forEachGuaranteed(operation: (T) -> Unit) {
    return iterator().forEachGuaranteed(operation)
}

/**
 * Applies [operation] to every element of this iterable and guarantees that all elements are visited.
 *
 * Exceptions thrown by [operation] are collected and rethrown after iteration as a
 * [CompoundRuntimeException].
 *
 * **Thread safety:** This function is not synchronized. It is safe for concurrent reads only if
 * this iterable is not mutated concurrently and [operation] is safe for concurrent use.
 *
 * @param operation the operation to apply to each element.
 * @throws CompoundRuntimeException if one or more invocations of [operation] throw.
 * @throws ConcurrentModificationException if the iterable iterator detects concurrent modification.
 */
inline fun <T> Iterable<T>.forEachGuaranteed(operation: (T) -> Unit) {
    return iterator().forEachGuaranteed(operation)
}

/**
 * Applies [operation] to every element of this collection and guarantees that all elements are visited.
 *
 * Exceptions thrown by [operation] are collected and rethrown after iteration as a
 * [CompoundRuntimeException].
 *
 * **Thread safety:** This function is not synchronized. It is safe for concurrent reads only if
 * this collection is not mutated concurrently and [operation] is safe for concurrent use.
 *
 * @param operation the operation to apply to each element.
 * @throws CompoundRuntimeException if one or more invocations of [operation] throw.
 * @throws ConcurrentModificationException if the collection iterator detects concurrent modification.
 */
inline fun <T> Iterator<T>.forEachGuaranteed(operation: (T) -> Unit) {
    var errors: MutableList<Throwable>? = null
    for (element in this) {
        try {
            operation(element)
        } catch (e: Throwable) {
            if (errors == null) {
                errors = ObjectArrayList()
            }
            errors.add(e)
        }
    }
    CompoundRuntimeException.throwIfNotEmpty(errors)
}

/**
 * Applies [operation] to every element of this collection and guarantees that all elements are visited.
 *
 * Exceptions thrown by [operation] are collected and rethrown after iteration as a
 * [CompoundRuntimeException].
 *
 * **Thread safety:** This function is not synchronized. It is safe for concurrent reads only if
 * this collection is not mutated concurrently and [operation] is safe for concurrent use.
 *
 * @param operation the operation to apply to each element.
 * @throws CompoundRuntimeException if one or more invocations of [operation] throw.
 * @throws ConcurrentModificationException if the collection iterator detects concurrent modification.
 */
inline fun <T> Iterable<T>.forEachLoggingErrors(logger: Logger, operation: (T) -> Unit) {
    for (it in this) {
        try {
            operation(it)
        } catch (e: Throwable) {
            logger.error(e.message, e)
        }
    }
    return
}

/**
 * Applies [operation] to every element of this collection and guarantees that all elements are visited.
 *
 * Exceptions thrown by [operation] are collected and rethrown after iteration as a
 * [CompoundRuntimeException].
 *
 * **Thread safety:** This function is not synchronized. It is safe for concurrent reads only if
 * this collection is not mutated concurrently and [operation] is safe for concurrent use.
 *
 * @param operation the operation to apply to each element.
 * @throws CompoundRuntimeException if one or more invocations of [operation] throw.
 * @throws ConcurrentModificationException if the collection iterator detects concurrent modification.
 */
inline fun <T, R : Any> Iterable<T>.mapNotNullLoggingErrors(
    logger: Logger,
    operation: (T) -> R?
): List<R> {
    return mapNotNull {
        try {
            operation(it)
        } catch (e: Throwable) {
            logger.error(e.message, e)
            null
        }
    }
}

/**
 * Adds [e] to this list when [e] is not `null`.
 *
 * **Thread safety:** This function is not synchronized. Concurrent safety depends on the list
 * implementation. Use external synchronization when the list may be mutated concurrently.
 *
 * @param e the element to add, or `null` to add nothing.
 */
fun <T> MutableList<T>.addIfNotNull(e: T?) {
    e?.let(::add)
}

/**
 * Adds all non-null [elements] to this list.
 *
 * **Thread safety:** This function is not synchronized. Concurrent safety depends on the list
 * implementation. Use external synchronization when the list may be mutated concurrently.
 *
 * @param elements the nullable elements to add.
 */
fun <T> MutableList<T>.addAllIfNotNull(vararg elements: T?) {
    elements.forEach { e -> e?.let(::add) }
}

/**
 * Transforms this array into a new typed array.
 *
 * **Thread safety:** This function is not synchronized. It is safe for concurrent reads only if
 * this array is not mutated concurrently and [transform] is safe for concurrent use.
 *
 * @param transform the transformation to apply to each element.
 * @return a new array containing the transformed elements.
 */
inline fun <T, reified R> Array<out T>.map2Array(transform: (T) -> R): Array<R> =
    Array(this.size) { i -> transform(this[i]) }

/**
 * Transforms this collection into a new typed array.
 *
 * **Thread safety:** This function is not synchronized. It is safe for concurrent reads only if
 * this collection is not mutated concurrently and [transform] is safe for concurrent use.
 *
 * @param transform the transformation to apply to each element.
 * @return a new array containing the transformed elements.
 * @throws ConcurrentModificationException if the collection iterator detects concurrent modification.
 */
inline fun <T, reified R> Collection<T>.map2Array(transform: (T) -> R): Array<R> {
    @Suppress("UNCHECKED_CAST")
    return arrayOfNulls<R>(this.size).also { array ->
        this.forEachIndexed { index, t -> array[index] = transform(t) }
    } as Array<R>
}

/**
 * Transforms this collection into a set.
 *
 * For empty and single-element collections, optimized set instances are returned.
 *
 * **Thread safety:** This function is not synchronized. It is safe for concurrent reads only if
 * this collection is not mutated concurrently and [transform] is safe for concurrent use. The
 * returned mutable fastutil set is exposed as [Set] and is not safe for concurrent mutation without
 * external synchronization.
 *
 * @param transform the transformation to apply to each element.
 * @return a set containing the transformed elements.
 * @throws ConcurrentModificationException if the collection iterator detects concurrent modification.
 */
inline fun <T, R> Collection<T>.mapSet(transform: (T) -> R): Set<R> {
    return when (val size = size) {
        1 -> {
            Collections.singleton(transform(first()))
        }

        0 -> emptySet()
        else -> mapTo(ObjectOpenHashSet(size), transform)
    }
}

/**
 * Creates an empty [EnumMap] for enum keys of type [E].
 *
 * **Thread safety:** The returned map is not synchronized and is not safe for concurrent mutation
 * without external synchronization.
 *
 * @return a mutable enum map keyed by [E].
 */
inline fun <reified E : Enum<E>, V> enumMapOf(): MutableMap<E, V> = EnumMap(E::class.java)

/**
 * Returns an iterator that yields elements from this iterator until [predicate] matches.
 *
 * The matching element is included in the returned sequence of elements. Iteration stops
 * immediately after yielding the first matching element.
 *
 * **Thread safety:** This function is not synchronized. Concurrent safety depends on the source
 * iterator implementation. Most iterators are not safe to use concurrently or while their backing
 * collection is being mutated.
 *
 * @param predicate returns `true` for the element after which iteration should stop.
 * @return an iterator that stops after the first element matching [predicate].
 * @throws ConcurrentModificationException if the source iterator detects concurrent modification.
 */
inline fun <T> Iterator<T>.stopAfter(crossinline predicate: (T) -> Boolean): Iterator<T> {
    return iterator {
        for (element in this@stopAfter) {
            yield(element)
            if (predicate(element)) {
                break
            }
        }
    }
}

/**
 * Returns the contained value, or `null` if this [Optional] is empty.
 *
 * **Thread safety:** This function does not mutate shared state. Concurrent safety depends only on
 * safe publication of the receiver [Optional] and the contained value.
 *
 * @return the contained value, or `null`.
 */
fun <T> Optional<T>.orNull(): T? = orElse(null)

/**
 * Replaces every element of this array with the result of applying [transform] to that element.
 *
 * The receiver array is mutated and returned.
 *
 * **Thread safety:** This function is not synchronized and is not safe for concurrent access to the
 * same array without external synchronization. The [transform] function must also be safe for the
 * calling context.
 *
 * @param transform the transformation used to replace each element.
 * @return this array after all elements have been transformed.
 */
fun <T> Array<T>.mapInPlace(transform: (T) -> T): Array<T> {
    for (i in this.indices) {
        this[i] = transform(this[i])
    }
    return this
}