package dev.slne.surf.api.core.random

import dev.slne.surf.api.core.random.RandomSelector.Companion.fromWeightedIterable
import kotlinx.coroutines.flow.Flow
import java.util.random.RandomGenerator

/**
 * A generic interface for selecting random elements from a collection with support for both uniform
 * and weighted probability distributions.
 *
 * [RandomSelector] provides an efficient and statistically correct implementation for random selection
 * using Apache Commons Math3's [EnumeratedDistribution]. It supports various collection types including
 * [Iterable] and [Flow], with optional custom [RandomGenerator] instances for reproducibility.
 *
 * ## Key Features
 * - **Uniform Selection**: Equal probability for all elements
 * - **Weighted Selection**: Custom probability distribution via weights
 * - **Multiple Output Formats**: Single picks, infinite flows, or sequences
 * - **Reproducibility**: Support for seeded random generators
 * - **Type Safety**: Full Kotlin generics support
 *
 * ## Usage Examples
 *
 * ### Example 1: Simple Uniform Selection
 * ```kotlin
 * val fruits = listOf("Apple", "Banana", "Cherry")
 * val selector = RandomSelector.fromIterable(fruits)
 *
 * val randomFruit = selector.pick()
 * println(randomFruit) // Each fruit has 33.3% probability
 * ```
 *
 * ### Example 2: Weighted Selection with Custom Weights
 * ```kotlin
 * data class Item(val name: String, val rarity: Double)
 *
 * val lootTable = listOf(
 *     Item("Common Sword", 1.0),
 *     Item("Rare Shield", 0.5),
 *     Item("Epic Helmet", 0.1)
 * )
 *
 * val selector = RandomSelector.fromWeightedIterable(lootTable) { it.rarity }
 * val drop = selector.pick() // Higher weight = higher probability
 * ```
 *
 * ### Example 3: Weighted Selection with Enums
 * ```kotlin
 * enum class Rarity(override val weight: Double) : Weighted {
 *     COMMON(1.0),      // 62.2% chance
 *     UNCOMMON(0.5),    // 31.1% chance
 *     RARE(0.1),        // 6.2% chance
 *     VERY_RARE(0.01)   // 0.6% chance
 *
 *     companion object {
 *         val selector = RandomSelector.fromWeightedIterable(entries)
 *     }
 * }
 *
 * repeat(100) {
 *     println(Rarity.selector.pick())
 * }
 * ```
 *
 * ### Example 4: Reproducible Results with Seeded Random
 * ```kotlin
 * val seed = 42L
 * val random = Random(seed).asJavaRandom()
 * val selector = RandomSelector.fromWeightedIterable(items, random) { it.weight }
 *
 * // Always produces the same sequence with the same seed
 * val result1 = selector.pick()
 * ```
 *
 * ### Example 5: Infinite Random Stream
 * ```kotlin
 * val selector = RandomSelector.fromIterable(listOf("A", "B", "C"))
 *
 * // Using Flow (for coroutines)
 * selector.flow().take(5).collect { println(it) }
 *
 * // Using Sequence (for synchronous code)
 * selector.sequence().take(5).forEach { println(it) }
 * ```
 *
 * ## Performance Characteristics
 * - **Creation**: O(n) where n is the number of elements
 * - **Selection**: O(log n) using binary search on cumulative weights
 * - **Memory**: O(n) for storing elements and cumulative distribution
 *
 * ## Thread Safety
 * Individual [RandomSelector] instances are NOT thread-safe. For concurrent access, either:
 * - Create separate selectors per thread with different [RandomGenerator] instances
 * - Use external synchronization
 * - Create a new selector from the same source data
 *
 * @param E The type of elements to be selected. Can be any type.
 * @see Weighted
 * @see Weighter
 */
interface RandomSelector<E> {

    /**
     * Selects and returns a single random element based on the configured probability distribution.
     *
     * This method is deprecated in favor of specifying the [RandomGenerator] at selector creation time.
     * The generator parameter here is only used for backward compatibility and creates a temporary
     * distribution instance.
     *
     * @param randomGenerator The random number generator to use for this selection.
     * @return A randomly selected element according to the probability distribution.
     * @see pick()
     */
    @Deprecated(
        message = "Use pick() without parameters. RandomGenerator can be specified at creation time.",
        replaceWith = ReplaceWith("pick()"),
        level = DeprecationLevel.WARNING
    )
    fun pick(randomGenerator: RandomGenerator): E

    /**
     * Selects and returns a single random element based on the configured probability distribution.
     *
     * For uniform selectors, each element has equal probability. For weighted selectors, elements
     * are selected with probability proportional to their weights.
     *
     * **Example:**
     * ```kotlin
     * val selector = RandomSelector.fromWeightedIterable(items) { it.weight }
     * val selected = selector.pick() // Returns one element
     * ```
     *
     * @return A randomly selected element according to the probability distribution.
     * @throws IllegalStateException if the selector is based on an infinite flow.
     */
    fun pick(): E

    /**
     * Selects and returns a random element, or null based on the success probability.
     *
     * This method adds an implicit "empty result" outcome to the probability distribution.
     * The success rate represents the probability of selecting an actual element (not null).
     *
     * ## Probability
     * - `P(element selected) = successRate`
     * - `P(null returned) = 1 - successRate`
     *
     * ## Use Cases
     * - **Fishing Systems**: Chance to catch something
     * - **Loot Drops**: Probability enemy drops an item
     * - **Random Encounters**: Event trigger chance
     * - **Gacha/Gatcha**: Pull success rate
     *
     * ## Examples
     *
     * **Fishing with 40% success rate:**
     * ```kotlin
     * val fishSelector = RandomSelector.fromWeightedIterable(fishTypes) { it.weight }
     * val caught = fishSelector.pickOrNull(successRate = 0.4) // 40% catch, 60% nothing
     *
     * when (caught) {
     *     null -> println("Nothing bites...")
     *     else -> println("You caught a $caught!")
     * }
     * ```
     *
     * **Loot with 25% drop rate:**
     * ```kotlin
     * val lootSelector = RandomSelector.fromWeightedIterable(items) { it.rarity }
     * val loot = lootSelector.pickOrNull(successRate = 0.25) // 25% drop rate
     *
     * if (loot != null) {
     *     player.inventory.add(loot)
     * }
     * ```
     *
     * **50/50 chance:**
     * ```kotlin
     * val result = selector.pickOrNull(successRate = 0.5)
     * ```
     *
     * @param successRate The probability of selecting an element (between 0.0 and 1.0).
     *                    0.0 = always returns null, 1.0 = always returns an element.
     * @return A randomly selected element, or null based on the success rate.
     * @throws IllegalArgumentException if successRate is not in range [0.0, 1.0].
     * @throws IllegalStateException if the selector is based on an infinite flow.
     */
    fun pickOrNull(successRate: Double): E?

    /**
     * Creates an infinite [Flow] that continuously emits random elements.
     *
     * This method is deprecated in favor of specifying the [RandomGenerator] at selector creation time.
     *
     * @param randomGenerator The random number generator to use for selections.
     * @return An infinite [Flow] of randomly selected elements.
     * @see flow()
     */
    @Deprecated(
        message = "Use flow() without parameters. RandomGenerator can be specified at creation time.",
        replaceWith = ReplaceWith("flow()"),
        level = DeprecationLevel.WARNING
    )
    fun flow(randomGenerator: RandomGenerator): Flow<E>

    /**
     * Creates an infinite [Flow] that continuously emits random elements.
     *
     * Each emitted element is selected independently according to the probability distribution.
     * This flow never completes and will emit elements indefinitely until cancelled.
     *
     * **Use cases:**
     * - Procedural generation in games (spawning enemies, loot drops)
     * - Simulation systems requiring continuous random events
     * - Testing with random data streams
     *
     * **Example:**
     * ```kotlin
     * val selector = RandomSelector.fromIterable(listOf("Red", "Green", "Blue"))
     *
     * selector.flow()
     *     .take(10)
     *     .collect { color ->
     *         println("Generated: $color")
     *     }
     * ```
     *
     * @return An infinite [Flow] of randomly selected elements.
     */
    fun flow(): Flow<E>

    /**
     * Creates an infinite [Flow] that continuously emits random elements or null values.
     *
     * This is equivalent to repeatedly calling [pickOrNull] with the specified success rate.
     * Each emission is independent, with the configured probability of being an actual element.
     *
     * **Example: Random Event Stream with Downtime**
     * ```kotlin
     * val eventSelector = RandomSelector.fromWeightedIterable(events) { it.frequency }
     *
     * eventSelector.flowOrNull(successRate = 0.3) // 30% events, 70% nothing
     *     .collect { event ->
     *         if (event != null) {
     *             println("Event triggered: $event")
     *         } else {
     *             println("No event this tick")
     *         }
     *     }
     * ```
     *
     * @param successRate The probability of emitting an element (between 0.0 and 1.0).
     * @return An infinite [Flow] of randomly selected elements or nulls.
     * @throws IllegalArgumentException if successRate is not in range [0.0, 1.0].
     */
    fun flowOrNull(successRate: Double): Flow<E?>

    /**
     * Creates an infinite [Sequence] that lazily generates random elements.
     *
     * Unlike [flow], this is a synchronous operation suitable for non-coroutine contexts.
     * The sequence is lazy and generates elements only when requested. It never terminates
     * unless explicitly limited with operations like [Sequence.take].
     *
     * **Performance Note:** Sequences are more efficient than flows for simple synchronous
     * iteration as they avoid coroutine overhead.
     *
     * **Example:**
     * ```kotlin
     * val selector = RandomSelector.fromWeightedIterable(dice) { it.weight }
     *
     * val rolls = selector.sequence()
     *     .take(100)
     *     .groupingBy { it }
     *     .eachCount()
     *
     * println("Distribution: $rolls")
     * ```
     *
     * @return An infinite [Sequence] of randomly selected elements.
     * @throws UnsupportedOperationException if the selector is based on an infinite flow.
     */
    fun sequence(): Sequence<E>

    /**
     * Creates an infinite [Sequence] that lazily generates random elements or null values.
     *
     * This is the sequence equivalent of [flowOrNull], useful for synchronous code that
     * needs nullable random results.
     *
     * **Example: Simulating Fishing Attempts**
     * ```kotlin
     * val fishSelector = RandomSelector.fromWeightedIterable(Fish.entries)
     *
     * val attempts = fishSelector.sequenceOrNull(successRate = 0.3) // 30% catch rate
     *     .take(50)
     *     .toList()
     *
     * val caught = attempts.filterNotNull()
     * println("Caught ${caught.size} fish in 50 attempts (expected ~15)")
     * ```
     *
     * @param successRate The probability of generating an element (between 0.0 and 1.0).
     * @return An infinite [Sequence] of randomly selected elements or nulls.
     * @throws IllegalArgumentException if successRate is not in range [0.0, 1.0].
     * @throws UnsupportedOperationException if the selector is based on an infinite flow.
     */
    fun sequenceOrNull(successRate: Double): Sequence<E?>

    companion object {

        /**
         * Creates a [RandomSelector] with uniform probability distribution from an [Iterable].
         *
         * All elements have equal probability of being selected (1/n where n is the element count).
         *
         * **Example:**
         * ```kotlin
         * val cards = listOf("Ace", "King", "Queen", "Jack")
         * val selector = RandomSelector.fromIterable(cards)
         * val card = selector.pick() // Each card has 25% probability
         * ```
         *
         * @param E The type of elements in the collection.
         * @param iterable The source collection of elements. Must not be empty.
         * @param randomGenerator Optional custom random generator for reproducible results.
         *                        If null, uses a default instance.
         * @return A [RandomSelector] with uniform selection probability.
         * @throws IllegalArgumentException if the iterable is empty.
         */
        @JvmOverloads
        fun <E> fromIterable(
            iterable: Iterable<E>,
            randomGenerator: RandomGenerator? = null
        ): RandomSelector<E> = RandomSelectorImpl.fromIterable(iterable, randomGenerator.toApache())

        /**
         * Creates a [RandomSelector] with uniform probability distribution from an [Iterable].
         *
         * This overload accepts an Apache Commons Math [org.apache.commons.math3.random.RandomGenerator]
         * directly for advanced use cases or interoperability with existing Apache Commons code.
         *
         * @param E The type of elements in the collection.
         * @param iterable The source collection of elements. Must not be empty.
         * @param randomGenerator Apache Commons Math random generator instance.
         * @return A [RandomSelector] with uniform selection probability.
         * @throws IllegalArgumentException if the iterable is empty.
         */
        fun <E> fromIterable(
            iterable: Iterable<E>,
            randomGenerator: org.apache.commons.math3.random.RandomGenerator
        ): RandomSelector<E> = RandomSelectorImpl.fromIterable(iterable, randomGenerator)

        /**
         * Creates a [RandomSelector] with custom weighted probability distribution from an [Iterable].
         *
         * Elements are selected with probability proportional to their weights. For example, an element
         * with weight 2.0 is twice as likely to be selected as an element with weight 1.0.
         *
         * **Weight Requirements:**
         * - All weights must be positive (> 0)
         * - All weights must be finite (not NaN or Infinity)
         * - Weights are relative; they don't need to sum to 1.0
         *
         * **Example:**
         * ```kotlin
         * data class Monster(val name: String, val spawnRate: Double)
         *
         * val monsters = listOf(
         *     Monster("Goblin", 10.0),    // 76.9% spawn chance
         *     Monster("Orc", 2.0),        // 15.4% spawn chance
         *     Monster("Dragon", 1.0)      // 7.7% spawn chance
         * )
         *
         * val selector = RandomSelector.fromWeightedIterable(monsters) { it.spawnRate }
         * val spawned = selector.pick()
         * ```
         *
         * @param E The type of elements in the collection.
         * @param iterable The source collection of elements. Must not be empty.
         * @param randomGenerator Optional custom random generator for reproducible results.
         * @param weighter A function that extracts or calculates the weight for each element.
         * @return A [RandomSelector] with weighted selection probability.
         * @throws IllegalArgumentException if the iterable is empty or any weight is invalid.
         */
        @JvmOverloads
        fun <E> fromWeightedIterable(
            iterable: Iterable<E>,
            randomGenerator: RandomGenerator? = null,
            weighter: Weighter<E>,
        ): RandomSelector<E> =
            RandomSelectorImpl.fromWeightedIterable(iterable, weighter, randomGenerator.toApache())

        /**
         * Creates a [RandomSelector] with custom weighted probability distribution from an [Iterable].
         *
         * This overload accepts an Apache Commons Math [org.apache.commons.math3.random.RandomGenerator]
         * directly for advanced use cases.
         *
         * @param E The type of elements in the collection.
         * @param iterable The source collection of elements. Must not be empty.
         * @param randomGenerator Apache Commons Math random generator instance.
         * @param weighter A function that extracts or calculates the weight for each element.
         * @return A [RandomSelector] with weighted selection probability.
         * @throws IllegalArgumentException if the iterable is empty or any weight is invalid.
         */
        fun <E> fromWeightedIterable(
            iterable: Iterable<E>,
            randomGenerator: org.apache.commons.math3.random.RandomGenerator,
            weighter: Weighter<E>,
        ): RandomSelector<E> =
            RandomSelectorImpl.fromWeightedIterable(iterable, weighter, randomGenerator)

        /**
         * Creates a [RandomSelector] from an [Iterable] of elements implementing [Weighted].
         *
         * This is a convenience method for types that already have a weight property. The [Weighted.weight]
         * property is used automatically without needing to specify a weighter function.
         *
         * **Example:**
         * ```kotlin
         * enum class ItemRarity(override val weight: Double) : Weighted {
         *     COMMON(100.0),
         *     UNCOMMON(20.0),
         *     RARE(5.0),
         *     LEGENDARY(1.0)
         * }
         *
         * val selector = RandomSelector.fromWeightedIterable(ItemRarity.entries)
         * ```
         *
         * @param E The type of elements, which must implement [Weighted].
         * @param iterable The source collection of weighted elements. Must not be empty.
         * @param randomGenerator Optional custom random generator for reproducible results.
         * @return A [RandomSelector] with weighted selection probability.
         * @throws IllegalArgumentException if the iterable is empty or any weight is invalid.
         */
        @JvmOverloads
        fun <E : Weighted> fromWeightedIterable(
            iterable: Iterable<E>,
            randomGenerator: RandomGenerator? = null
        ): RandomSelector<E> =
            RandomSelectorImpl.fromWeightedIterable(
                iterable,
                { it.weight },
                randomGenerator.toApache()
            )

        /**
         * Creates a [RandomSelector] from an [Iterable] of elements implementing [Weighted].
         *
         * This overload accepts an Apache Commons Math [org.apache.commons.math3.random.RandomGenerator]
         * directly.
         *
         * @param E The type of elements, which must implement [Weighted].
         * @param iterable The source collection of weighted elements. Must not be empty.
         * @param randomGenerator Apache Commons Math random generator instance.
         * @return A [RandomSelector] with weighted selection probability.
         * @throws IllegalArgumentException if the iterable is empty or any weight is invalid.
         */
        fun <E : Weighted> fromWeightedIterable(
            iterable: Iterable<E>,
            randomGenerator: org.apache.commons.math3.random.RandomGenerator
        ): RandomSelector<E> =
            RandomSelectorImpl.fromWeightedIterable(iterable, { it.weight }, randomGenerator)

        /**
         * Creates a [RandomSelector] from a **finite** weighted [Flow].
         *
         * This suspending function collects all elements from the flow into memory before creating
         * the selector. The flow **must** be finite; infinite flows will cause this function to
         * suspend indefinitely.
         *
         * **Use cases:**
         * - Creating selectors from database queries
         * - Building selectors from asynchronous API responses
         * - Processing streamed data that needs random selection
         *
         * **Warning:** All elements are collected into memory. For large datasets, consider using
         * [fromWeightedIterable] with a pre-materialized collection instead.
         *
         * **Example:**
         * ```kotlin
         * suspend fun loadItemsFromDatabase(): Flow<Item> = ...
         *
         * val selector = RandomSelector.fromFlow(
         *     flow = loadItemsFromDatabase(),
         *     weighter = { it.dropChance }
         * )
         * ```
         *
         * @param E The type of elements in the flow.
         * @param flow The finite source flow of elements. Must emit at least one element.
         * @param randomGenerator Optional custom random generator for reproducible results.
         * @param weighter A function that extracts or calculates the weight for each element.
         * @return A [RandomSelector] with weighted selection probability.
         * @throws IllegalArgumentException if the flow is empty or any weight is invalid.
         */
        @JvmOverloads
        suspend fun <E> fromFlow(
            flow: Flow<E>,
            randomGenerator: RandomGenerator? = null,
            weighter: Weighter<E>
        ): RandomSelector<E> =
            RandomSelectorImpl.fromFlow(flow, weighter, randomGenerator.toApache())

        /**
         * Creates a [RandomSelector] from a **finite** weighted [Flow].
         *
         * This overload accepts an Apache Commons Math [org.apache.commons.math3.random.RandomGenerator]
         * directly.
         *
         * @param E The type of elements in the flow.
         * @param flow The finite source flow of elements. Must emit at least one element.
         * @param randomGenerator Apache Commons Math random generator instance.
         * @param weighter A function that extracts or calculates the weight for each element.
         * @return A [RandomSelector] with weighted selection probability.
         * @throws IllegalArgumentException if the flow is empty or any weight is invalid.
         */
        suspend fun <E> fromFlow(
            flow: Flow<E>,
            randomGenerator: org.apache.commons.math3.random.RandomGenerator,
            weighter: Weighter<E>
        ): RandomSelector<E> =
            RandomSelectorImpl.fromFlow(flow, weighter, randomGenerator)
    }
}