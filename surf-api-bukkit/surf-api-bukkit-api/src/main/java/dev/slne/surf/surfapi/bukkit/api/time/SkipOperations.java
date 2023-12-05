package dev.slne.surf.surfapi.bukkit.api.time;

import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


/**
 * The SkipOperations class provides static fields representing different skip operators for calculating time intervals
 * in a Minecraft world.
 */
@ApiStatus.NonExtendable
public final class SkipOperations {

    /**
     * Represents a skip operator for calculating the time interval until the next sunrise in a Minecraft world.
     * The value of this variable is a lambda expression that takes a World object as input and calculates the time to add to the world.
     */
    public static final SkipOperation NEXT_SUNRISE = world -> normalizeTimeToAdd(calculateMargin(world, 22600L));
    /**
     * Represents a skip operator for calculating the time interval until the next sunset in a Minecraft world.
     * The value of this variable is a lambda expression that takes a World object as input and calculates the time to add to the world.
     */
    public static final SkipOperation NEXT_SUNSET = world -> normalizeTimeToAdd(calculateMargin(world, 11800L));
    /**
     * Represents a skip operator for calculating the time interval until the next morning in a Minecraft world.
     * The value of this variable is a lambda expression that takes a World object as input and calculates the time to add to the world.
     */
    public static final SkipOperation NEXT_DAY = world -> normalizeTimeToAdd(calculateMargin(world, 500L));
    /**
     * Represents a skip operator for calculating the time interval until the next noon in a Minecraft world.
     * The value of this variable is a lambda expression that takes a World object as input and calculates the time to add to the world.
     */
    public static final SkipOperation NEXT_NOON = world -> normalizeTimeToAdd(calculateMargin(world, 6000L));
    /**
     * Represents a skip operator for calculating the time interval until the next afternoon in a Minecraft world.
     * The value of this variable is a lambda expression that takes a World object as input and calculates the time to add to the world.
     */
    public static final SkipOperation NEXT_NIGHT = world -> normalizeTimeToAdd(calculateMargin(world, 13000L));
    /**
     * Represents a skip operator for calculating the time interval until the next midnight in a Minecraft world.
     * The value of this variable is a lambda expression that takes a World object as input and calculates the time to add to the world.
     */
    public static final SkipOperation NEXT_MIDNIGHT = world -> normalizeTimeToAdd(calculateMargin(world, 18000L));

    /**
     * Calculates the margin between the given dayTime and the full time of the world.
     *
     * @param world The World object representing the Minecraft world.
     * @param dayTime The time of the day in ticks (0-23999).
     * @return The margin between the dayTime and the full time of the world.
     */
    private static long calculateMargin(@NotNull World world, long dayTime) {
        return (dayTime - world.getFullTime()) % 24000L;
    }

    /**
     * Normalizes the time to add based on a given margin. If the margin is negative, it adds 24000 to make it positive.
     *
     * @param margin The time margin to normalize
     * @return The normalized time margin
     */
    @Contract(pure = true)
    private static long normalizeTimeToAdd(long margin) {
        if (margin < 0L) {
            margin += 24000L;
        }

        return margin;
    }

    /**
     * A functional interface representing a skip operator for calculating time intervals in a Minecraft world.
     */
    @FunctionalInterface
    public interface SkipOperation {
        /**
         * Calculates the time to add to a Minecraft world.
         *
         * @param world The Minecraft world for which the time needs to be calculated.
         * @return The time to add to the world, represented as a long value.
         */
        long timeToAdd(@NotNull World world);
    }
}
