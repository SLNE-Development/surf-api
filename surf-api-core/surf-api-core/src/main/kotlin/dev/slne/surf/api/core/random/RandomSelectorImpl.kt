package dev.slne.surf.api.core.random

import dev.slne.surf.api.core.util.mutableObjectListOf
import it.unimi.dsi.fastutil.doubles.DoubleArrayList
import kotlinx.coroutines.flow.Flow
import org.apache.commons.math3.random.Well19937c
import java.util.random.RandomGenerator

internal class RandomSelectorImpl<E>(
    randomGenerator: org.apache.commons.math3.random.RandomGenerator?,
    elements: Iterable<E>,
    weighter: Weighter<E>,
) : RandomSelector<E> {
    private val random = randomGenerator ?: Well19937c()
    private val values: Array<Any?>
    private val cumulativeWeights: DoubleArray
    private val totalWeight: Double

    init {
        var capacity = (elements as? Collection<*>)?.size ?: 10
        if (capacity == 0) {
            throw IllegalArgumentException("RandomSelector must have at least one element.")
        }
        capacity = capacity.coerceAtLeast(1)

        var collectedValues = arrayOfNulls<Any?>(capacity)
        var weights = DoubleArray(capacity)
        var size = 0
        var maxWeight = 0.0
        for (element in elements) {
            val weight = weighter(element)
            require(weight > 0.0) { "Weight must be greater than 0, got $weight for element $element." }
            require(weight.isFinite()) { "Weight must be finite, got $weight for element $element." }

            if (size == collectedValues.size) {
                val newSize = size + (size shr 1) + 1
                collectedValues = collectedValues.copyOf(newSize)
                weights = weights.copyOf(newSize)
            }
            collectedValues[size] = element
            weights[size] = weight
            size++
            if (weight > maxWeight) maxWeight = weight
        }
        require(size > 0) { "RandomSelector must have at least one element." }

        values = if (size == collectedValues.size) collectedValues else collectedValues.copyOf(size)

        cumulativeWeights = DoubleArray(size)
        var cumulativeWeight = 0.0
        for (index in 0 until size) {
            // Scaling prevents a finite set of individually finite weights from overflowing.
            cumulativeWeight += weights[index] / maxWeight
            cumulativeWeights[index] = cumulativeWeight
        }
        totalWeight = cumulativeWeight
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun pick(randomGenerator: RandomGenerator): E {
        return sample(randomGenerator.nextDouble())
    }

    override fun pick(): E {
        return sample(random.nextDouble())
    }

    override fun pickOrNull(successRate: Double): E? {
        require(successRate in 0.0..1.0) {
            "Success rate must be between 0.0 and 1.0, got $successRate."
        }

        return if (random.nextDouble() < successRate) {
            pick()
        } else {
            null
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun flow(randomGenerator: RandomGenerator): Flow<E> = kotlinx.coroutines.flow.flow {
        while (true) {
            emit(sample(randomGenerator.nextDouble()))
        }
    }

    override fun flow(): Flow<E> = kotlinx.coroutines.flow.flow {
        while (true) {
            emit(pick())
        }
    }

    override fun flowOrNull(successRate: Double): Flow<E?> = kotlinx.coroutines.flow.flow {
        require(successRate in 0.0..1.0) {
            "Success rate must be between 0.0 and 1.0, got $successRate."
        }

        while (true) {
            emit(if (random.nextDouble() < successRate) pick() else null)
        }
    }

    override fun sequence(): Sequence<E> = generateSequence {
        pick()
    }

    override fun sequenceOrNull(successRate: Double): Sequence<E?> {
        require(successRate in 0.0..1.0) {
            "Success rate must be between 0.0 and 1.0, got $successRate."
        }

        return sequence {
            while (true) {
                yield(if (random.nextDouble() < successRate) pick() else null)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun sample(unitSample: Double): E {
        val target = unitSample * totalWeight
        var low = 0
        var high = cumulativeWeights.lastIndex
        while (low < high) {
            val middle = (low + high) ushr 1
            if (target < cumulativeWeights[middle]) {
                high = middle
            } else {
                low = middle + 1
            }
        }
        return values[low] as E
    }

    companion object {
        fun <E> fromIterable(
            iterable: Iterable<E>,
            randomGenerator: org.apache.commons.math3.random.RandomGenerator?
        ): RandomSelectorImpl<E> {
            return RandomSelectorImpl(randomGenerator, iterable) { 1.0 }
        }

        fun <E> fromWeightedIterable(
            iterable: Iterable<E>,
            weighter: Weighter<E>,
            randomGenerator: org.apache.commons.math3.random.RandomGenerator?
        ): RandomSelectorImpl<E> {
            return RandomSelectorImpl(randomGenerator, iterable, weighter)
        }

        suspend fun <E> fromFlow(
            flow: Flow<E>,
            weighter: Weighter<E>,
            randomGenerator: org.apache.commons.math3.random.RandomGenerator?
        ): RandomSelectorImpl<E> {
            val elements = mutableObjectListOf<E>()
            val weights = DoubleArrayList()

            flow.collect { element ->
                val weight = weighter(element)
                require(weight > 0) { "Weight must be greater than 0, got $weight for element $element." }
                require(weight.isFinite()) { "Weight must be finite, got $weight for element $element." }
                elements.add(element)
                weights.add(weight)
            }

            var index = 0
            return RandomSelectorImpl(randomGenerator, elements) { weights.getDouble(index++) }
        }

        fun <E> fromInfinityFlow(
            flow: Flow<E>,
            weighter: Weighter<E>,
            randomGenerator: org.apache.commons.math3.random.RandomGenerator?
        ): RandomSelector<E> {
            return FlowRandomSelectorImpl(flow, randomGenerator ?: Well19937c(), weighter)
        }
    }
}

internal class FlowRandomSelectorImpl<E>(
    private val flow: Flow<E>,
    private val randomGenerator: org.apache.commons.math3.random.RandomGenerator,
    private val weighter: Weighter<E>,
) : RandomSelector<E> {
    @Suppress("OVERRIDE_DEPRECATION")
    override fun pick(randomGenerator: RandomGenerator): E {
        throw UnsupportedOperationException("FlowRandomSelector does not support pick operation. Use flow() instead.")
    }

    override fun pick(): E {
        throw UnsupportedOperationException("FlowRandomSelector does not support pick operation. Use flow() instead.")
    }

    override fun pickOrNull(successRate: Double): E? {
        throw UnsupportedOperationException("FlowRandomSelector does not support pickOrNull operation.")
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun flow(randomGenerator: RandomGenerator): Flow<E> =
        createFlow(randomGenerator.toApache())

    override fun flow(): Flow<E> = createFlow(randomGenerator)
    override fun flowOrNull(successRate: Double): Flow<E?> =
        throw UnsupportedOperationException("FlowRandomSelector does not support flowOrNull operation.")

    override fun sequence(): Sequence<E> {
        throw UnsupportedOperationException("FlowRandomSelector does not support sequence operation.")
    }

    override fun sequenceOrNull(successRate: Double): Sequence<E?> {
        throw UnsupportedOperationException("FlowRandomSelector does not support sequenceOrNull operation.")
    }

    private fun createFlow(
        generator: org.apache.commons.math3.random.RandomGenerator
    ): Flow<E> = kotlinx.coroutines.flow.flow {
        var selectedElement: E? = null
        var hasSelectedElement = false
        var maxWeight = 0.0
        var normalizedTotalWeight = 0.0

        flow.collect { element ->
            val weight = weighter(element)
            require(weight > 0.0) { "Weight must be greater than 0, got $weight." }
            require(weight.isFinite()) { "Weight must be finite, got $weight." }

            if (weight > maxWeight) {
                if (maxWeight > 0.0) {
                    normalizedTotalWeight *= maxWeight / weight
                }
                maxWeight = weight
            }
            val normalizedWeight = weight / maxWeight
            normalizedTotalWeight += normalizedWeight
            val threshold = normalizedWeight / normalizedTotalWeight

            if (generator.nextDouble() < threshold) {
                selectedElement = element
                hasSelectedElement = true
            }

            if (hasSelectedElement) {
                @Suppress("UNCHECKED_CAST")
                emit(selectedElement as E)
            }
        }
    }
}
