package dev.slne.surf.api.core.collection

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

    override fun addAll(elements: Collection<M>): Boolean {
        var modified = false
        for (element in elements) {
            val transformed = transformFrom(element) ?: continue
            modified = fromSet.add(transformed) || modified
        }
        return modified
    }

    override fun removeAll(elements: Collection<M>): Boolean {
        var modified = false
        for (element in elements) {
            val transformed = transformFrom(element) ?: continue
            modified = fromSet.remove(transformed) || modified
        }
        return modified
    }

    override fun retainAll(elements: Collection<M>) =
        fromSet.retainAll(elements.mapNotNullTo(HashSet(elements.size), ::transformFrom))

    override fun clear() = fromSet.clear()
    override val size: Int get() = fromSet.count { transformTo(it) != null }
    override fun isEmpty() = fromSet.none { transformTo(it) != null }
    override fun contains(element: M) = transformFrom(element)?.let(fromSet::contains) == true
    override fun containsAll(elements: Collection<M>) = elements.all(::contains)

    private fun transformFrom(element: M) = if (element != null) fromTransformer(element) else null
    private fun transformTo(element: O) = if (element != null) toTransformer(element) else null
}
