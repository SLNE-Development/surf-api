package dev.slne.surf.surfapi.core.api.random

import dev.slne.surf.surfapi.core.api.util.collectionSizeOrDefault
import dev.slne.surf.surfapi.core.api.util.mutableDoubleListOf
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import dev.slne.surf.surfapi.core.api.util.toObjectList
import it.unimi.dsi.fastutil.doubles.DoubleList
import it.unimi.dsi.fastutil.objects.ObjectList
import kotlinx.coroutines.flow.Flow
import java.util.random.RandomGenerator

internal class RandomSelectorImpl<E>(
    private val cumulativeWeights: DoubleList,
    private val elements: ObjectList<E>,
) : RandomSelector<E> {
    init {
        require(elements.isNotEmpty()) { "RandomSelector must have at least one element." }
    }

    override fun pick(randomGenerator: RandomGenerator): E {
        val totalWeight = cumulativeWeights.getDouble(cumulativeWeights.size - 1)
        val randomValue = randomGenerator.nextDouble(totalWeight)

        val index = cumulativeWeights.binarySearch { if (it < randomValue) -1 else 0 }
            .let { if (it < 0) -(it + 1) else it }

        return elements[index]
    }

    override fun flow(randomGenerator: RandomGenerator): Flow<E> = kotlinx.coroutines.flow.flow {
        while (true) {
            emit(pick(randomGenerator))
        }
    }

    companion object {
        fun <E> fromIterable(iterable: Iterable<E>): RandomSelectorImpl<E> {
            val elements = iterable.toObjectList()
            val cumulativeWeights =
                DoubleList.of(*DoubleArray(elements.size) { (it + 1).toDouble() })
            return RandomSelectorImpl(cumulativeWeights, elements)
        }

        fun <E> fromWeightedIterable(
            iterable: Iterable<E>,
            weighter: Weighter<E>,
        ): RandomSelectorImpl<E> {
            val expectedSize = iterable.collectionSizeOrDefault(10)
            val elements = mutableObjectListOf<E>(expectedSize)
            val cumulativeWeights = mutableDoubleListOf(expectedSize)
            var cumulativeWeight = 0.0

            for (element in iterable) {
                val weight = weighter(element)
                require(weight > 0) { "Weight must be greater than 0." }
                cumulativeWeight += weight
                cumulativeWeights.add(cumulativeWeight)
                elements.add(element)
            }

            return RandomSelectorImpl(cumulativeWeights, elements)
        }

        suspend fun <E> fromFlow(flow: Flow<E>, weighter: Weighter<E>): RandomSelectorImpl<E> {
            val elements = mutableObjectListOf<E>()
            val cumulativeWeights = mutableDoubleListOf()
            var cumulativeWeight = 0.0

            flow.collect { element ->
                val weight = weighter(element)
                require(weight > 0) { "Weight must be greater than 0." }
                cumulativeWeight += weight
                cumulativeWeights.add(cumulativeWeight)
                elements.add(element)
            }

            return RandomSelectorImpl(cumulativeWeights, elements)
        }

        fun <E> fromInfinityFlow(flow: Flow<E>, weighter: Weighter<E>): RandomSelector<E> {
            return FlowRandomSelectorImpl(flow, weighter)
        }
    }
}

internal class FlowRandomSelectorImpl<E>(
    private val flow: Flow<E>,
    private val weighter: Weighter<E>,
) : RandomSelector<E> {
    override fun pick(randomGenerator: RandomGenerator): E {
        throw UnsupportedOperationException("FlowRandomSelector does not support pick operation.")
    }

    override fun flow(randomGenerator: RandomGenerator): Flow<E> = kotlinx.coroutines.flow.flow {
        var selectedElement: E? = null
        var totalWeight = 0.0

        flow.collect { element ->
            val weight = weighter(element)
            require(weight > 0) { "Weight must be greater than 0." }

            totalWeight += weight
            val probability = weight / totalWeight

            if (randomGenerator.nextDouble() < probability) {
                selectedElement = element
            }

            emit(selectedElement!!)
        }
    }
}