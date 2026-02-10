package dev.slne.surf.surfapi.core.api.random

/**
 * A type alias for functions that compute the weight of an element for weighted random selection.
 *
 * The [Weighter] function takes an element and returns a positive [Double] representing its
 * relative weight in the probability distribution. Higher weights increase selection probability.
 *
 * ## Weight Requirements
 * - Must return a value greater than 0.0
 * - Must return a finite value (not NaN or Infinity)
 * - Weights are relative; they don't need to sum to 1.0
 *
 * ## Example Usage
 *
 * ```kotlin
 * data class QuestReward(val item: String, val rarity: Int)
 *
 * val rewards = listOf(
 *     QuestReward("Gold Coins", rarity = 10),
 *     QuestReward("Magic Potion", rarity = 5),
 *     QuestReward("Legendary Sword", rarity = 1)
 * )
 *
 * // Weighter that converts rarity to probability weight
 * val weighter: Weighter<QuestReward> = { reward ->
 *     1.0 / reward.rarity // Rarer items have lower weight
 * }
 *
 * val selector = RandomSelector.fromWeightedIterable(rewards, weighter = weighter)
 * val reward = selector.pick()
 * ```
 *
 * ## Common Weighter Patterns
 *
 * **Direct Property Access:**
 * ```kotlin
 * val selector = RandomSelector.fromWeightedIterable(items) { it.weight }
 * ```
 *
 * **Computed Weight:**
 * ```kotlin
 * val selector = RandomSelector.fromWeightedIterable(players) { player ->
 *     player.level * player.experienceMultiplier
 * }
 * ```
 *
 * **Inverse Probability:**
 * ```kotlin
 * val selector = RandomSelector.fromWeightedIterable(tasks) { task ->
 *     1.0 / task.priority // Lower priority number = higher selection chance
 * }
 * ```
 *
 * @param E The type of the element being weighted.
 * @return A positive, finite Double representing the element's weight.
 * @see Weighted
 * @see RandomSelector.fromWeightedIterable
 */
typealias Weighter<E> = (E) -> Double

/**
 * Interface for objects that have an intrinsic weight for weighted random selection.
 *
 * Classes implementing [Weighted] can be used directly with [RandomSelector.fromWeightedIterable]
 * without needing to provide a separate [Weighter] function. This is particularly useful for
 * enums, sealed classes, and domain objects where weight is a natural property.
 *
 * ## Implementation Guidelines
 *
 * The [weight] property:
 * - Must always return a positive value (> 0.0)
 * - Must always return a finite value (not NaN or Infinity)
 * - Should be immutable for consistent selection probabilities
 * - Represents relative probability (doesn't need to sum to 1.0)
 *
 * ## Example: Enum with Weights
 *
 * ```kotlin
 * enum class LootRarity(override val weight: Double) : Weighted {
 *     COMMON(100.0),      // 79.4% drop chance
 *     UNCOMMON(20.0),     // 15.9% drop chance
 *     RARE(5.0),          // 4.0% drop chance
 *     LEGENDARY(1.0);     // 0.8% drop chance
 *
 *     companion object {
 *         // Reusable selector for the enum
 *         val selector = RandomSelector.fromWeightedIterable(entries)
 *     }
 * }
 *
 * fun dropLoot(): LootRarity = LootRarity.selector.pick()
 * ```
 *
 * ## Example: Data Class with Dynamic Weight
 *
 * ```kotlin
 * data class Enemy(
 *     val name: String,
 *     val level: Int,
 *     val spawnMultiplier: Double
 * ) : Weighted {
 *     override val weight: Double
 *         get() = level * spawnMultiplier
 * }
 *
 * val enemies = listOf(
 *     Enemy("Goblin", level = 1, spawnMultiplier = 2.0),
 *     Enemy("Orc", level = 5, spawnMultiplier = 1.0),
 *     Enemy("Dragon", level = 20, spawnMultiplier = 0.1)
 * )
 *
 * val selector = RandomSelector.fromWeightedIterable(enemies)
 * ```
 *
 * ## Example: Sealed Class Hierarchy
 *
 * ```kotlin
 * sealed class RandomEvent(override val weight: Double) : Weighted {
 *     data object SunnyDay : RandomEvent(10.0)
 *     data object RainyDay : RandomEvent(5.0)
 *     data object Thunderstorm : RandomEvent(2.0)
 *     data object Earthquake : RandomEvent(0.1)
 *
 *     companion object {
 *         val allEvents = listOf(SunnyDay, RainyDay, Thunderstorm, Earthquake)
 *         val selector = RandomSelector.fromWeightedIterable(allEvents)
 *     }
 * }
 * ```
 *
 * @see Weighter
 * @see RandomSelector.fromWeightedIterable
 */
interface Weighted {

    /**
     * The weight of this object for weighted random selection.
     *
     * Higher values increase the probability of this element being selected.
     * The weight is relative to other elements in the same selection pool.
     *
     * **Requirements:**
     * - Must be > 0.0
     * - Must be finite (not NaN or Infinity)
     * - Should remain constant for predictable selection behavior
     *
     * **Probability Calculation:**
     * ```
     * P(element) = element.weight / sum(all weights)
     * ```
     *
     * @return A positive, finite Double representing the selection weight.
     */
    val weight: Double
}