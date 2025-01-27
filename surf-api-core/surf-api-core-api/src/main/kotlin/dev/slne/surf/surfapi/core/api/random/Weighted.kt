package dev.slne.surf.surfapi.core.api.random

/**
 * A type alias for a function that computes the weight of an object for weighted random selection.
 *
 * The [Weighter] is a function that takes an element of type [E] and returns a [Double]
 * representing its weight. It is used in cases where elements do not implement [Weighted]
 * directly, allowing custom weight computation.
 *
 * ### Example
 * ```
 * data class Item(val name: String, val importance: Double)
 *
 * val items = listOf(
 *     Item("Basic", 1.0),
 *     Item("Advanced", 2.0),
 *     Item("Premium", 5.0)
 * )
 *
 * val selector = RandomSelector.fromWeightedIterable(items) { it.importance }
 *
 * println(selector.pick()) // Outputs an item based on its importance
 * ```
 *
 * @param E The type of the object for which the weight is being computed.
 */
typealias Weighter<E> = (E) -> Double

/**
 * Represents an object that has an associated weight used for weighted random selection.
 *
 * Classes implementing this interface can be used directly with methods like
 * [RandomSelector.fromWeightedIterable], where the weight of the object determines its likelihood
 * of being selected.
 *
 * ### Example
 * ```
 * enum class Rarity(override val weight: Double) : Weighted {
 *     COMMON(1.0),
 *     UNCOMMON(0.5),
 *     RARE(0.1)
 * }
 *
 * val selector = RandomSelector.fromWeightedIterable(Rarity.entries)
 *
 * println(selector.pick()) // Outputs a random Rarity based on the weights
 * ```
 */
interface Weighted {

    /**
     * The weight of the object. Higher values increase the likelihood of being selected.
     */
    val weight: Double
}