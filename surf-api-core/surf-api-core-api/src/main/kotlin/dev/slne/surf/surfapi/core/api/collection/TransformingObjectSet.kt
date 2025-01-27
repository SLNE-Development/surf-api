package dev.slne.surf.surfapi.core.api.collection

import it.unimi.dsi.fastutil.objects.ObjectIterator
import it.unimi.dsi.fastutil.objects.ObjectSet

class TransformingObjectSet<O, M>(
    private val fromSet: ObjectSet<O>,
    private val toTransformer: (O) -> M?,
    private val fromTransformer: (M) -> O?,
) : ObjectSet<M> {
    override fun iterator() = object : ObjectIterator<M> {
        private val iterator = fromSet.iterator()
        override fun remove() = iterator.remove()
        override fun next(): M? = toTransformer(iterator.next())
        override fun hasNext() = iterator.hasNext()
    }

    override fun add(element: M?) = fromSet.add(transformFrom(element))
    override fun remove(element: M) = fromSet.remove(transformFrom(element))
    override fun addAll(elements: Collection<M?>) = fromSet.addAll(elements.mapNotNull(::transformFrom))
    override fun removeAll(elements: Collection<M?>) = fromSet.removeAll(elements.mapNotNull(::transformFrom))
    override fun retainAll(elements: Collection<M?>) = fromSet.retainAll(elements.mapNotNull(::transformFrom))
    override fun clear() = fromSet.clear()
    override val size: Int get() = fromSet.size
    override fun isEmpty() = fromSet.isEmpty()
    override fun contains(element: M?) = fromSet.contains(transformFrom(element))
    override fun containsAll(elements: Collection<M?>) = fromSet.containsAll(elements.mapNotNull(::transformFrom))

    private fun transformFrom(element: M?) = if (element != null) fromTransformer(element) else null
    private fun transformTo(element: O?) = if (element != null) toTransformer(element) else null
}