package dev.slne.surf.api.common.random

import kotlinx.coroutines.flow.Flow
import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.apache.commons.math3.random.Well19937c
import org.apache.commons.math3.util.Pair
import java.util.AbstractMap.SimpleImmutableEntry

internal class RandomSelectorImpl<E>(
    randomGenerator: org.apache.commons.math3.random.RandomGenerator?,
    elements: List<Map.Entry<E, Double>>,
) : RandomSelector<E> {
    private val distribution: EnumeratedDistribution<E>
    private val random = randomGenerator ?: Well19937c()

    init {
        require(elements.isNotEmpty()) { "RandomSelector must have at least one element." }

        val pmf = elements.map { Pair.create(it.key, it.value) }
        distribution = EnumeratedDistribution(random, pmf)
    }

    override fun pick(): E {
        return distribution.sample()
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

    override fun flow(): Flow<E> {
        return kotlinx.coroutines.flow.flow {
            while (true) {
                emit(pick())
            }
        }
    }

    override fun flowOrNull(successRate: Double): Flow<E?> {
        return kotlinx.coroutines.flow.flow {
            require(successRate in 0.0..1.0) {
                "Success rate must be between 0.0 and 1.0, got $successRate."
            }

            while (true) {
                emit(pickOrNull(successRate))
            }
        }
    }

    override fun sequence(): Sequence<E> {
        return generateSequence {
            pick()
        }
    }

    override fun sequenceOrNull(successRate: Double): Sequence<E?> {
        require(successRate in 0.0..1.0) {
            "Success rate must be between 0.0 and 1.0, got $successRate."
        }

        return generateSequence {
            pickOrNull(successRate)
        }
    }

    companion object {
        fun <E> fromIterable(
            iterable: Iterable<E>,
            randomGenerator: org.apache.commons.math3.random.RandomGenerator?,
        ): RandomSelectorImpl<E> {
            val elements = iterable.map { SimpleImmutableEntry(it, 1.0) }

            return RandomSelectorImpl(randomGenerator, elements)
        }

        fun <E> fromWeightedIterable(
            iterable: Iterable<E>,
            weighter: Weighter<E>,
            randomGenerator: org.apache.commons.math3.random.RandomGenerator?,
        ): RandomSelectorImpl<E> {
            val elements = iterable.map { element ->
                val weight = weighter(element)
                require(weight > 0) {
                    "Weight must be greater than 0, got $weight for element $element."
                }
                require(weight.isFinite()) {
                    "Weight must be finite, got $weight for element $element."
                }
                SimpleImmutableEntry(element, weight)
            }

            return RandomSelectorImpl(randomGenerator, elements)
        }

        suspend fun <E> fromFlow(
            flow: Flow<E>,
            weighter: Weighter<E>,
            randomGenerator: org.apache.commons.math3.random.RandomGenerator?,
        ): RandomSelectorImpl<E> {
            val elements = mutableListOf<Map.Entry<E, Double>>()

            flow.collect { element ->
                val weight = weighter(element)
                require(weight > 0) {
                    "Weight must be greater than 0, got $weight for element $element."
                }
                require(weight.isFinite()) {
                    "Weight must be finite, got $weight for element $element."
                }
                elements.add(SimpleImmutableEntry(element, weight))
            }

            return RandomSelectorImpl(randomGenerator, elements)
        }

        fun <E> fromInfinityFlow(
            flow: Flow<E>,
            weighter: Weighter<E>,
            randomGenerator: org.apache.commons.math3.random.RandomGenerator?,
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
    override fun pick(): E {
        throw UnsupportedOperationException(
            "FlowRandomSelector does not support pick operation. Use flow() instead.",
        )
    }

    override fun pickOrNull(successRate: Double): E? {
        throw UnsupportedOperationException(
            "FlowRandomSelector does not support pickOrNull operation.",
        )
    }

    override fun flow(): Flow<E> = createFlow(randomGenerator)
    override fun flowOrNull(successRate: Double): Flow<E?> {
        throw UnsupportedOperationException(
            "FlowRandomSelector does not support flowOrNull operation.",
        )
    }

    override fun sequence(): Sequence<E> {
        throw UnsupportedOperationException(
            "FlowRandomSelector does not support sequence operation.",
        )
    }

    override fun sequenceOrNull(successRate: Double): Sequence<E?> {
        throw UnsupportedOperationException(
            "FlowRandomSelector does not support sequenceOrNull operation.",
        )
    }

    private fun createFlow(
        generator: org.apache.commons.math3.random.RandomGenerator,
    ): Flow<E> {
        return kotlinx.coroutines.flow.flow {
            var selectedElement: E? = null
            var totalWeight = 0.0

            flow.collect { element ->
                val weight = weighter(element)
                require(weight > 0.0) { "Weight must be greater than 0, got $weight." }
                require(weight.isFinite()) { "Weight must be finite, got $weight." }

                totalWeight += weight
                val threshold = weight / totalWeight

                if (generator.nextDouble() < threshold) {
                    selectedElement = element
                }

                emit(selectedElement!!)
            }
        }
    }
}