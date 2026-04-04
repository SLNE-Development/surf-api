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
        override fun remove() = iterator.remove()
        override fun next(): M? = toTransformer(iterator.next())
        override fun hasNext() = iterator.hasNext()
    }

    /**
     * Adds a transformed element to the underlying set.
     *
     * @param element The element of type `M` to add.
     * @return `true` if the underlying set was modified, `false` otherwise.
     */
    override fun add(element: M?) = fromSet.add(transformFrom(element))

    /**
     * Removes a transformed element from the underlying set.
     *
     * @param element The element of type `M` to remove.
     * @return `true` if the element was removed, `false` otherwise.
     */
    override fun remove(element: M) = fromSet.remove(transformFrom(element))

    /**
     * Adds all transformed elements from the given collection to the underlying set.
     *
     * @param elements A collection of elements of type `M`.
     * @return `true` if the underlying set was modified, `false` otherwise.
     */
    override fun addAll(elements: Collection<M?>) =
        fromSet.addAll(elements.mapNotNull(::transformFrom))

    /**
     * Removes all transformed elements in the given collection from the underlying set.
     *
     * @param elements A collection of elements of type `M`.
     * @return `true` if the underlying set was modified, `false` otherwise.
     */
    override fun removeAll(elements: Collection<M?>) =
        fromSet.removeAll(elements.mapNotNull(::transformFrom))

    /**
     * Retains only the transformed elements in the given collection in the underlying set.
     *
     * @param elements A collection of elements of type `M`.
     * @return `true` if the underlying set was modified, `false` otherwise.
     */
    override fun retainAll(elements: Collection<M?>) =
        fromSet.retainAll(elements.mapNotNull(::transformFrom))

    /**
     * Removes all elements from the underlying set.
     */
    override fun clear() = fromSet.clear()

    /**
     * Returns the number of elements in the transformed set.
     *
     * @return The size of the underlying set.
     */
    override val size: Int get() = fromSet.size

    /**
     * Checks if the transformed set is empty.
     *
     * @return `true` if the underlying set is empty, `false` otherwise.
     */
    override fun isEmpty() = fromSet.isEmpty()

    /**
     * Checks if the transformed set contains the specified element.
     *
     * @param element The element of type `M` to check.
     * @return `true` if the element is in the transformed set, `false` otherwise.
     */
    override fun contains(element: M?) = fromSet.contains(transformFrom(element))

    /**
     * Checks if the transformed set contains all elements in the specified collection.
     *
     * @param elements A collection of elements of type `M`.
     * @return `true` if all elements are in the transformed set, `false` otherwise.
     */
    override fun containsAll(elements: Collection<M?>) =
        fromSet.containsAll(elements.mapNotNull(::transformFrom))

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
