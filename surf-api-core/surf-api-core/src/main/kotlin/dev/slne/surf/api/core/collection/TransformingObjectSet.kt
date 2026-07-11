package dev.slne.surf.api.core.api.collection

import it.unimi.dsi.fastutil.objects.ObjectIterator
import it.unimi.dsi.fastutil.objects.ObjectSet

/**
 * A specialized implementation of [ObjectSet] that transforms elements between two types, `O` and `M`, using
 * the provided transformation functions. This enables a dynamic view of a set where elements are transformed
 * on-the-fly during iteration or when performing operations.
 *
 * @param O The type of the elements in the underlying set.
 * @param M The type of the transformed elements in this set.
 * @property fromSet The underlying set containing elements of type `O`.
 * @property toTransformer A function that transforms elements of type `O` to type `M`. Can return null to filter out elements.
 * @property fromTransformer A function that transforms elements of type `M` to type `O`. Can return null to filter out elements.
 */
class TransformingObjectSet<O, M>(
    private val fromSet: ObjectSet<O>,
    private val toTransformer: (O) -> M?,
    private val fromTransformer: (M) -> O?,
) : ObjectSet<M> {

    /**
     * Returns an iterator that transforms elements from the underlying set during iteration.
     *
     * @return An iterator over the transformed elements of type `M`.
     */
    override fun iterator() = object : ObjectIterator<M> {
        private val iterator = fromSet.iterator()
        private var nextValue: M? = null
        private var nextComputed = false

        override fun remove() = iterator.remove()

        override fun next(): M {
            if (!hasNext()) throw NoSuchElementException()
            nextComputed = false
            val result = nextValue
            nextValue = null
            @Suppress("UNCHECKED_CAST")
            return result as M
        }

        override fun hasNext(): Boolean {
            if (nextComputed) return nextValue != null

            while (iterator.hasNext()) {
                val transformed = transformTo(iterator.next())
                if (transformed != null) {
                    nextValue = transformed
                    nextComputed = true
                    return true
                }
            }

            nextValue = null
            nextComputed = true
            return false
        }
    }

    /**
     * Adds a transformed element to the underlying set.
     *
     * @param element The element of type `M` to add.
     * @return `true` if the underlying set was modified, `false` otherwise.
     */
    override fun add(element: M?) = transformFrom(element)?.let(fromSet::add) == true

    /**
     * Removes a transformed element from the underlying set.
     *
     * @param element The element of type `M` to remove.
     * @return `true` if the element was removed, `false` otherwise.
     */
    override fun remove(element: M) = transformFrom(element)?.let(fromSet::remove) == true

    /**
     * Adds all transformed elements from the given collection to the underlying set.
     *
     * @param elements A collection of elements of type `M`.
     * @return `true` if the underlying set was modified, `false` otherwise.
     */
    override fun addAll(elements: Collection<M?>): Boolean {
        var modified = false
        for (element in elements) {
            val transformed = transformFrom(element) ?: continue
            modified = fromSet.add(transformed) || modified
        }
        return modified
    }

    /**
     * Removes all transformed elements in the given collection from the underlying set.
     *
     * @param elements A collection of elements of type `M`.
     * @return `true` if the underlying set was modified, `false` otherwise.
     */
    override fun removeAll(elements: Collection<M?>): Boolean {
        var modified = false
        for (element in elements) {
            val transformed = transformFrom(element) ?: continue
            modified = fromSet.remove(transformed) || modified
        }
        return modified
    }

    /**
     * Retains only the transformed elements in the given collection in the underlying set.
     *
     * @param elements A collection of elements of type `M`.
     * @return `true` if the underlying set was modified, `false` otherwise.
     */
    override fun retainAll(elements: Collection<M?>) =
        fromSet.retainAll(elements.mapNotNullTo(HashSet(elements.size), ::transformFrom))

    /**
     * Removes all elements from the underlying set.
     */
    override fun clear() = fromSet.clear()

    /**
     * Returns the number of elements in the transformed set.
     *
     * @return The number of underlying elements that can be transformed.
     */
    override val size: Int get() = fromSet.count { transformTo(it) != null }

    /**
     * Checks if the transformed set is empty.
     *
     * @return `true` if the transformed view has no visible elements, `false` otherwise.
     */
    override fun isEmpty() = fromSet.none { transformTo(it) != null }

    /**
     * Checks if the transformed set contains the specified element.
     *
     * @param element The element of type `M` to check.
     * @return `true` if the element is in the transformed set, `false` otherwise.
     */
    override fun contains(element: M?) = transformFrom(element)?.let(fromSet::contains) == true

    /**
     * Checks if the transformed set contains all elements in the specified collection.
     *
     * @param elements A collection of elements of type `M`.
     * @return `true` if all elements are in the transformed set, `false` otherwise.
     */
    override fun containsAll(elements: Collection<M?>) = elements.all(::contains)

    /**
     * Transforms an element of type `M` to type `O` using the provided [fromTransformer].
     *
     * @param element The element of type `M`.
     * @return The transformed element of type `O`, or `null` if the transformation is not possible.
     */
    private fun transformFrom(element: M?) = if (element != null) fromTransformer(element) else null

    /**
     * Transforms an element of type `O` to type `M` using the provided [toTransformer].
     *
     * @param element The element of type `O`.
     * @return The transformed element of type `M`, or `null` if the transformation is not possible.
     */
    private fun transformTo(element: O?) = if (element != null) toTransformer(element) else null
}
