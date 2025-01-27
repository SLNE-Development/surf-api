package dev.slne.surf.surfapi.core.api.random

import dev.slne.surf.surfapi.core.api.util.random
import kotlinx.coroutines.flow.Flow
import java.util.random.RandomGenerator

/**
 * A generic interface for selecting random elements from a collection, supporting both uniform and weighted selection.
 *
 * The [RandomSelector] provides a flexible way to pick elements randomly, either using equal probabilities
 * or custom weights, and supports various collection types including [Iterable] and [Flow].
 *
 * ### Example 1: Simple Random Selection
 * ```
 * val items = listOf("Apple", "Banana", "Cherry")
 * val selector = RandomSelector.fromIterable(items)
 *
 * println(selector.pick()) // Prints a random item from the list
 * ```
 *
 * ### Example 2: Weighted Random Selection
 * ```
 * data class Item(val name: String, val weight: Double)
 * val items = listOf(
 *     Item("Common", 1.0),
 *     Item("Uncommon", 0.5),
 *     Item("Rare", 0.1)
 * )
 *
 * val selector = RandomSelector.fromWeightedIterable(items) { it.weight }
 *
 * println(selector.pick()) // Prints an item with probability proportional to its weight
 * ```
 *
 * ### Example 3: Weighted Random Selection with Enums
 * ```
 * enum class Rarity(override val weight: Double) : Weighted {
 *     COMMON(1.0),
 *     UNCOMMON(0.5),
 *     RARE(0.1),
 *     VERY_RARE(0.01)
 *
 *     companion object {
 *         val selector = RandomSelector.fromWeightedIterable(entries)
 *     }
 * }
 *
 * for (i in 1..10) {
 *     println(Rarity.selector.pick()) // Prints a random rarity based on its weight
 * }
 * ```
 *
 * ### Example 4: Infinite Random Flow
 * ```
 * val items = listOf("A", "B", "C")
 * val selector = RandomSelector.fromIterable(items)
 * val randomFlow = selector.flow()
 *
 * randomFlow.take(5).collect { println(it) } // Continuously emits random items
 * ```
 *
 * @param E The type of elements to be selected.
 */
interface RandomSelector<E> {

    /**
     * Picks a single random element from the collection.
     *
     * @param randomGenerator The random number generator to use. Defaults to a shared instance.
     * @return A randomly selected element.
     */
    fun pick(randomGenerator: RandomGenerator = random): E

    /**
     * Creates an infinite [Flow] that emits random elements.
     *
     * @param randomGenerator The random number generator to use. Defaults to a shared instance.
     * @return A [Flow] of randomly selected elements.
     */
    fun flow(randomGenerator: RandomGenerator = random): Flow<E>

    companion object {

        /**
         * Creates a [RandomSelector] from an [Iterable], using uniform selection probabilities.
         *
         * @param iterable The iterable collection of elements.
         * @return A [RandomSelector] that selects elements uniformly at random.
         */
        fun <E> fromIterable(iterable: Iterable<E>): RandomSelector<E> =
            RandomSelectorImpl.fromIterable(iterable)

        /**
         * Creates a [RandomSelector] from a weighted [Iterable].
         *
         * @param iterable The iterable collection of elements.
         * @param weighter A function that determines the weight of each element.
         * @return A [RandomSelector] that selects elements based on their weights.
         */
        fun <E> fromWeightedIterable(
            iterable: Iterable<E>,
            weighter: Weighter<E>,
        ): RandomSelector<E> =
            RandomSelectorImpl.fromWeightedIterable(iterable, weighter)

        /**
         * Creates a [RandomSelector] from a weighted [Iterable] of elements that implement [Weighted].
         *
         * @param iterable The iterable collection of elements.
         * @return A [RandomSelector] that selects elements based on their weights.
         */
        fun <E : Weighted> fromWeightedIterable(iterable: Iterable<E>): RandomSelector<E> =
            RandomSelectorImpl.fromWeightedIterable(iterable) { it.weight }

        /**
         * Creates a [RandomSelector] from a weighted **finite** [Flow].
         *
         * @param flow The **finite** flow of elements.
         * @param weighter A function that determines the weight of each element.
         * @return A [RandomSelector] that selects elements based on their weights.
         */
        suspend fun <E> fromFlow(flow: Flow<E>, weighter: Weighter<E>): RandomSelector<E> =
            RandomSelectorImpl.fromFlow(flow, weighter)

        /**
         * Creates a [RandomSelector] from an infinite weighted [Flow].
         *
         * ##### Note
         * The returned [RandomSelector] does not support [pick] operation. Use [flow] to consume elements.
         *
         * @param flow The flow of elements.
         * @param weighter A function that determines the weight of each element.
         * @return A [RandomSelector] that emits elements based on their weights.
         */
        fun <E> fromInfinityFlow(flow: Flow<E>, weighter: Weighter<E>): RandomSelector<E> =
            RandomSelectorImpl.fromInfinityFlow(flow, weighter)
    }
}

enum class ExampleWeightedEnum(override val weight: Double) : Weighted {
    COMMON(1.0),
    UNCOMMON(0.5),
    RARE(0.1),
    VERY_RARE(0.01),
    EXTREMELY_RARE(0.001);

    companion object {
        val selector = RandomSelector.fromWeightedIterable(entries)
    }
}

fun main() {
    for (i in 1..10) {
        println(ExampleWeightedEnum.selector.pick())
    }
}