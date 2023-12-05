package dev.slne.surf.surfapi.bukkit.api;

import dev.slne.surf.surfapi.bukkit.api.packet.SurfBukkitPacketApi;
import dev.slne.surf.surfapi.bukkit.api.scoreboard.SurfScoreboardBuilder;
import dev.slne.surf.surfapi.bukkit.api.time.SkipOperations;
import dev.slne.surf.surfapi.bukkit.api.time.TimeSkipResult;
import dev.slne.surf.surfapi.bukkit.api.visualizer.SurfBukkitVisualizerApi;
import dev.slne.surf.surfapi.core.api.SurfCoreApi;
import dev.slne.surf.surfapi.core.api.util.Result;
import net.kyori.adventure.text.Component;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Represents the API for SurfBukkit.
 */
@ApiStatus.NonExtendable
public interface SurfBukkitApi extends SurfCoreApi {

    /**
     * Retrieves the specific SurfBukkitPacketApi instance.
     *
     * @return the SurfBukkitPacketApi instance
     */
    @Override
    SurfBukkitPacketApi getPacketApi();

    SurfBukkitVisualizerApi getVisualizerApi();

    /**
     * Retrieves the {@link ScoreboardLibrary} instance.
     *
     * @return the {@link ScoreboardLibrary} instance
     */
    ScoreboardLibrary getScoreboardLibrary();

    /**
     * Creates a SurfScoreboardBuilder with the given title.
     *
     * @param title the title of the scoreboard
     * @return a SurfScoreboardBuilder with the given title
     */
    SurfScoreboardBuilder createScoreboard(@NotNull Component title);

    /**
     * Sends a player to a specified server.
     *
     * @param player The player to send.
     * @param server The name of the server to send the player to.
     */
    default void sendPlayerToServer(@NotNull Player player, String server) {
        sendPlayerToServer(player.getUniqueId(), server);
    }

    /**
     * Skips time smoothly in a given world by adding the specified amount of time.
     *
     * @param world     the world in which to skip time
     * @param timeToAdd the amount of time to add (in ticks)
     * @return a Result object containing the TimeSkipResult
     */
    Result<TimeSkipResult> skipTimeSmoothly(World world, int timeToAdd);

    /**
     * Smoothly skips time in the specified world by adding the given time for the specified duration.
     *
     * @param world      the world in which to skip time
     * @param timeToAdd  the amount of time to add
     * @param duration   the duration of the time skip operation
     * @return a Result object representing the result of the time skip operation
     */
    Result<TimeSkipResult> skipTimeSmoothly(World world, long timeToAdd, long duration);

    /**
     * Smoothly skips time in the specified Minecraft world based on the given skip operation.
     *
     * @param world         The Minecraft world in which to skip time.
     * @param skipOperation The type of time skip operation to perform.
     *                      Use one of the skip operation constants defined in the SkipOperations class.
     * @return A Result object that contains the result of the time skip operation.
     * @see SkipOperations
     */
    Result<TimeSkipResult> skipTimeSmoothly(World world, SkipOperations.SkipOperation skipOperation);

    /**
     * Skips time smoothly in the specified worlds.
     *
     * @param timeToAdd the amount of time to add to each world
     * @return a map containing the result of the time skip operation for each world, with the world as the key and the result as the value
     */
    Map<World, Result<TimeSkipResult>> skipTimeSmoothly(int timeToAdd);

    /**
     * Skip time smoothly by adding a specified amount of time to each world for a given duration.
     *
     * @param timeToAdd  the amount of time to add to each world
     * @param duration   the duration of the time skip operation
     * @return a map containing the results of the time skip operation for each world,
     *         where the key is the world and the value is the result of the operation
     */
    Map<World, Result<TimeSkipResult>> skipTimeSmoothly(long timeToAdd, long duration);

    /**
     * Skips time smoothly in a Minecraft world based on the specified skip operation.
     *
     * @param skipOperation The skip operation to calculate the time interval to skip.
     *                      It should implement the {@link SkipOperations.SkipOperation} functional interface.
     * @return A map associating each affected world with the result of the time skip operation.
     *         The result is represented by the {@link Result<TimeSkipResult>} class.
     * @see SkipOperations
     */
    Map<World, Result<TimeSkipResult>> skipTimeSmoothly(SkipOperations.SkipOperation skipOperation);

    /**
     * Retrieves the instance of SurfBukkitApi.
     *
     * @return the instance of SurfBukkitApi
     * @throws NullPointerException if the SurfBukkitApi instance has not been initialized yet
     */
    @Contract(pure = true)
    static SurfBukkitApi get() {
        return SurfBukkitApiAccess.getInstance();
    }
}
