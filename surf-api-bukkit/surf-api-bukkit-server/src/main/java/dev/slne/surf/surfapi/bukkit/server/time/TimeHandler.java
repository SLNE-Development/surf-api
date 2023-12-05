package dev.slne.surf.surfapi.bukkit.server.time;

import dev.slne.surf.surfapi.bukkit.api.time.TimeSkipResult;
import dev.slne.surf.surfapi.bukkit.server.BukkitMain;
import dev.slne.surf.surfapi.core.api.util.Result;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


/**
 * The TimeHandler class provides methods for skipping time smoothly in a Minecraft world.
 */
public class TimeHandler {

    /**
     * The DEFAULT_SKIP_AMOUNT is a constant variable that specifies the default amount of time to skip in the TimeHandler class.
     * It is of type long and has a value of 100L.
     */
    public static final long DEFAULT_SKIP_AMOUNT = 100L;
    /**
     * The INSTANCE variable is a static final variable of type TimeHandler.
     * It represents a singleton instance of the TimeHandler class.
     * The TimeHandler class provides methods for skipping time smoothly in a Minecraft world.
     */
    public static final TimeHandler INSTANCE = new TimeHandler();
    /**
     * Represents the list of worlds where time skipping is currently in progress.
     * <p>
     * This list is used to keep track of worlds where the time is being skipped smoothly using the {@link TimeHandler} class.
     * Synchronization is ensured using {@link Collections#synchronizedList(List)} to make it thread-safe.
     */
    private final List<UUID> skippingWorlds = Collections.synchronizedList(new ArrayList<>());

    /**
     * The TimeHandler class provides methods for skipping time smoothly in a Minecraft world.
     */
    private TimeHandler() {
    }


    /**
     * Skips time smoothly in a Minecraft world.
     *
     * @param world       the world in which time will be skipped
     * @param timeToAdd   the amount of time to add to the current time of the world
     * @param duration    the duration over which the time will be skipped
     * @return a Result object containing the result of the time skip operation
     */
    public Result<TimeSkipResult> skipTimeSmoothly(World world, long timeToAdd, long duration) {
        final Result<TimeSkipResult> result = new Result<>();
        final long targetTime = world.getFullTime() + timeToAdd;
        final long step = timeToAdd / duration;

        if (skippingWorlds.contains(world.getUID())) {
            result.complete(TimeSkipResult.ALREADY_SKIPPING);
            return result;
        }

        skippingWorlds.add(world.getUID());
        Bukkit.getScheduler().runTaskTimer(BukkitMain.getInstance(), bukkitTask -> {
            long newTime = world.getFullTime() + step;

            if (newTime >= targetTime) {
                bukkitTask.cancel();
                world.setFullTime(targetTime);
                skippingWorlds.remove(world.getUID());
                result.complete(TimeSkipResult.SUCCESS);
            } else {
                world.setFullTime(newTime);
            }

        }, 0, 1);

        return result;
    }
}
