package dev.slne.surf.surfapi.core.api.collection

import it.unimi.dsi.fastutil.objects.ObjectIterator
import it.unimi.dsi.fastutil.objects.ObjectSet

class TransformingSet2ObjectSet<O, M>(
    private val fromSet: MutableSet<O>,
    private val toTransformer: (O) -> M?,
    private val fromTransformer: (M) -> O?,
) : ObjectSet<M> {
    override fun iterator() = object : ObjectIterator<M> {
        private val iterator = fromSet.iterator()
        private var nextValue: M? = null
        private var nextComputed: Boolean = false

        override fun hasNext(): Boolean {
            if (nextComputed) {
                return nextValue != null
            }
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

        override fun next(): M {
            if (!hasNext()) {
                throw NoSuchElementException()
            }
            nextComputed = false
            val result = nextValue
            nextValue = null
            @Suppress("UNCHECKED_CAST")
            return result as M
        }
        override fun remove() = iterator.remove()
    }

    override fun add(element: M): Boolean = transformFrom(element)?.let(fromSet::add) == true
    override fun remove(element: M) = transformFrom(element)?.let(fromSet::remove) == true

    override fun addAll(elements: Collection<M>) =
        fromSet.addAll(elements.mapNotNull(::transformFrom))

    override fun removeAll(elements: Collection<M>) =
        fromSet.removeAll(elements.mapNotNull(::transformFrom).toSet())

    override fun retainAll(elements: Collection<M>) =
        fromSet.retainAll(elements.mapNotNull(::transformFrom).toSet())

    override fun clear() = fromSet.clear()
    override val size: Int get() = fromSet.size
    override fun isEmpty() = fromSet.isEmpty()
    override fun contains(element: M) = fromSet.contains(transformFrom(element))
    override fun containsAll(elements: Collection<M>) = fromSet.containsAll(elements.mapNotNull(::transformFrom))

    private fun transformFrom(element: M) = if (element != null) fromTransformer(element) else null
    private fun transformTo(element: O) = if (element != null) toTransformer(element) else null
}