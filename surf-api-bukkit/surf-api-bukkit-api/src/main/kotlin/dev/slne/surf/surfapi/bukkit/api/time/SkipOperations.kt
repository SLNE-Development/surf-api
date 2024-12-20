package dev.slne.surf.surfapi.bukkit.api.time

import org.bukkit.World
import org.jetbrains.annotations.Contract

/**
 * The SkipOperations class provides static fields representing different skip operators for
 * calculating time intervals in a Minecraft world.
 */
object SkipOperations {
    /**
     * Represents a skip operator for calculating the time interval until the next sunrise in a
     * Minecraft world. The value of this variable is a lambda expression that takes a World object as
     * input and calculates the time to add to the world.
     */
    @JvmField
    val NEXT_SUNRISE = SkipOperation { normalizeTimeToAdd(calculateMargin(it, 22600L)) }

    /**
     * Represents a skip operator for calculating the time interval until the next sunset in a
     * Minecraft world. The value of this variable is a lambda expression that takes a World object as
     * input and calculates the time to add to the world.
     */
    @JvmField
    val NEXT_SUNSET = SkipOperation { normalizeTimeToAdd(calculateMargin(it, 11800L)) }

    /**
     * Represents a skip operator for calculating the time interval until the next morning in a
     * Minecraft world. The value of this variable is a lambda expression that takes a World object as
     * input and calculates the time to add to the world.
     */
    @JvmField
    val NEXT_DAY = SkipOperation { normalizeTimeToAdd(calculateMargin(it, 500L)) }

    /**
     * Represents a skip operator for calculating the time interval until the next noon in a Minecraft
     * world. The value of this variable is a lambda expression that takes a World object as input and
     * calculates the time to add to the world.
     */
    @JvmField
    val NEXT_NOON = SkipOperation { normalizeTimeToAdd(calculateMargin(it, 6000L)) }

    /**
     * Represents a skip operator for calculating the time interval until the next afternoon in a
     * Minecraft world. The value of this variable is a lambda expression that takes a World object as
     * input and calculates the time to add to the world.
     */
    @JvmField
    val NEXT_NIGHT = SkipOperation { normalizeTimeToAdd(calculateMargin(it, 13000L)) }

    /**
     * Represents a skip operator for calculating the time interval until the next midnight in a
     * Minecraft world. The value of this variable is a lambda expression that takes a World object as
     * input and calculates the time to add to the world.
     */
    @JvmField
    val NEXT_MIDNIGHT = SkipOperation { normalizeTimeToAdd(calculateMargin(it, 18000L)) }

    /**
     * Calculates the margin between the given dayTime and the full time of the world.
     *
     * @param world   The World object representing the Minecraft world.
     * @param dayTime The time of the day in ticks (0-23999).
     * @return The margin between the dayTime and the full time of the world.
     */
    private fun calculateMargin(world: World, dayTime: Long): Long {
        return (dayTime - world.fullTime) % 24000L
    }

    /**
     * Normalizes the time to add based on a given margin. If the margin is negative, it adds 24000 to
     * make it positive.
     *
     * @param margin The time margin to normalize
     * @return The normalized time margin
     */
    @Contract(pure = true)
    private fun normalizeTimeToAdd(margin: Long): Long {
        var margin = margin
        if (margin < 0L) {
            margin += 24000L
        }

        return margin
    }

    /**
     * A functional interface representing a skip operator for calculating time intervals in a
     * Minecraft world.
     */
    fun interface SkipOperation {
        /**
         * Calculates the time to add to a Minecraft world.
         *
         * @param world The Minecraft world for which the time needs to be calculated.
         * @return The time to add to the world, represented as a long value.
         */
        fun timeToAdd(world: World): Long
    }
}
