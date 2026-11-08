package dev.slne.surf.api.core.random

import dev.slne.surf.api.core.util.mutableObjectListOf
import it.unimi.dsi.fastutil.objects.Object2DoubleMap
import kotlinx.coroutines.flow.Flow
import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.apache.commons.math3.random.Well19937c
import org.apache.commons.math3.util.Pair
import java.util.random.RandomGenerator

internal class RandomSelectorImpl<E>(
    randomGenerator: org.apache.commons.math3.random.RandomGenerator?,
    elements: List<Object2DoubleMap.Entry<E>>
) : RandomSelector<E> {
    private val distribution: EnumeratedDistribution<E>
    private val random = randomGenerator ?: Well19937c()

    init {
        require(elements.isNotEmpty()) { "RandomSelector must have at least one element." }

        val pmf = elements.map { Pair.create(it.key, it.doubleValue) }
        distribution = EnumeratedDistribution(random, pmf)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun pick(randomGenerator: RandomGenerator): E {
        // Create temporary distribution with provided generator for backward compatibility
        val tempDistribution = EnumeratedDistribution(
            Jdk2ApacheRandomGenerator(randomGenerator),
            distribution.pmf
        )
        return tempDistribution.sample()
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

    @Suppress("OVERRIDE_DEPRECATION")
    override fun flow(randomGenerator: RandomGenerator): Flow<E> = kotlinx.coroutines.flow.flow {
        // Create temporary distribution with provided generator for backward compatibility
        val tempDistribution = EnumeratedDistribution(
            Jdk2ApacheRandomGenerator(randomGenerator),
            distribution.pmf
        )
        while (true) {
            emit(tempDistribution.sample())
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
            emit(pickOrNull(successRate))
        }
    }

    override fun sequence(): Sequence<E> = generateSequence {
        pick()
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
            randomGenerator: org.apache.commons.math3.random.RandomGenerator?
        ): RandomSelectorImpl<E> {
            val elements = iterable.map { Object2DoubleMap.entry(it, 1.0) }
            return RandomSelectorImpl(randomGenerator, elements)
        }

        fun <E> fromWeightedIterable(
            iterable: Iterable<E>,
            weighter: Weighter<E>,
            randomGenerator: org.apache.commons.math3.random.RandomGenerator?
        ): RandomSelectorImpl<E> {
            val elements = iterable.map { element ->
                val weight = weighter(element)
                require(weight > 0) { "Weight must be greater than 0, got $weight for element $element." }
                require(weight.isFinite()) { "Weight must be finite, got $weight for element $element." }
                Object2DoubleMap.entry(element, weight)
            }

            return RandomSelectorImpl(randomGenerator, elements)
        }

        suspend fun <E> fromFlow(
            flow: Flow<E>,
            weighter: Weighter<E>,
            randomGenerator: org.apache.commons.math3.random.RandomGenerator?
        ): RandomSelectorImpl<E> {
            val elements = mutableObjectListOf<Object2DoubleMap.Entry<E>>()

            flow.collect { element ->
                val weight = weighter(element)
                require(weight > 0) { "Weight must be greater than 0, got $weight for element $element." }
                require(weight.isFinite()) { "Weight must be finite, got $weight for element $element." }
                elements.add(Object2DoubleMap.entry(element, weight))
            }

            return RandomSelectorImpl(randomGenerator, elements)
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