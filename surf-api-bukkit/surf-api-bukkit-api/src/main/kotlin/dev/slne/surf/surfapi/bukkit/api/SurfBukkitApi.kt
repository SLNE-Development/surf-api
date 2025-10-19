package dev.slne.surf.surfapi.bukkit.api

import dev.slne.surf.surfapi.bukkit.api.scoreboard.ObsoleteScoreboardApi
import dev.slne.surf.surfapi.bukkit.api.scoreboard.SurfScoreboardBuilder
import dev.slne.surf.surfapi.bukkit.api.time.SkipOperations.SkipOperation
import dev.slne.surf.surfapi.bukkit.api.time.TimeSkipResult
import dev.slne.surf.surfapi.core.api.SurfCoreApi
import dev.slne.surf.surfapi.core.api.toast.Toast
import dev.slne.surf.surfapi.core.api.toast.ToastBuilder
import dev.slne.surf.surfapi.core.api.toast.ToastStyle
import net.kyori.adventure.text.Component
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.jetbrains.annotations.ApiStatus

/**
 * Represents the API for SurfBukkit.
 */
@ApiStatus.NonExtendable
interface SurfBukkitApi : SurfCoreApi {

    @ObsoleteScoreboardApi
    fun scoreboardLibrary(): ScoreboardLibrary

    /**
     * Creates a SurfScoreboardBuilder with the given title.
     *
     * @param title the title of the scoreboard
     * @return a SurfScoreboardBuilder with the given title
     */
    @ObsoleteScoreboardApi
    fun createScoreboard(title: Component): SurfScoreboardBuilder

    /**
     * Sends a player to a specified server.
     *
     * @param player The player to send.
     * @param server The name of the server to send the player to.
     */
    fun sendPlayerToServer(player: Player, server: String) {
        sendPlayerToServer(player.uniqueId, server)
    }

    /**
     * Skips time smoothly in a given world by adding the specified amount of time.
     * Suspends the coroutine until the time skip operation is complete.
     *
     * @param world     the world in which to skip time
     * @param timeToAdd the amount of time to add (in ticks)
     * @return the TimeSkipResult
     */
    suspend fun skipTimeSmoothly(world: World, timeToAdd: Long): TimeSkipResult

    /**
     * Smoothly skips time in the specified world by adding the given time for the specified
     * duration.
     *
     * @param world     the world in which to skip time
     * @param timeToAdd the amount of time to add
     * @param duration  the duration of the time skip operation
     * @return a Result object representing the result of the time skip operation
     */
    suspend fun skipTimeSmoothly(world: World, timeToAdd: Long, duration: Long): TimeSkipResult

    /**
     * Smoothly skips time in the specified Minecraft world based on the given skip operation.
     *
     * @param world         The Minecraft world in which to skip time.
     * @param skipOperation The type of time skip operation to perform. Use one of the skip operation
     * constants defined in the SkipOperations class.
     * @return A Result object that contains the result of the time skip operation.
     * @see SkipOperations
     */
    suspend fun skipTimeSmoothly(world: World, skipOperation: SkipOperation): TimeSkipResult

    /**
     * Skips time smoothly in the specified worlds.
     *
     * @param timeToAdd the amount of time to add to each world
     * @return a map containing the result of the time skip operation for each world, with the world
     * as the key and the result as the value
     */
    suspend fun skipTimeSmoothly(timeToAdd: Long): Map<World, TimeSkipResult>

    /**
     * Skip time smoothly by adding a specified amount of time to each world for a given duration.
     *
     * @param timeToAdd the amount of time to add to each world
     * @param duration  the duration of the time skip operation
     * @return a map containing the results of the time skip operation for each world, where the key
     * is the world and the value is the result of the operation
     */
    suspend fun skipTimeSmoothly(
        timeToAdd: Long,
        duration: Long
    ): Map<World, TimeSkipResult>

    /**
     * Skips time smoothly in a Minecraft world based on the specified skip operation.
     *
     * @param skipOperation The skip operation to calculate the time interval to skip. It should
     * implement the [SkipOperations.SkipOperation] functional interface.
     * @return A map associating each affected world with the result of the time skip operation. The
     * result is represented by the [<] class.
     * @see SkipOperations
     */
    suspend fun skipTimeSmoothly(skipOperation: SkipOperation): Map<World, TimeSkipResult>

    /**
     * Creates a Toast using the provided builder function.
     *
     * @param builder A lambda function that configures the ToastBuilder.
     * @return The created Toast instance.
     *
     * @see ToastBuilder
     */
    fun createToast(builder: ToastBuilder.() -> Unit): Toast

    /**
     * Creates a Toast with the specified icon, display text, and style.
     *
     * @param icon The icon material for the toast.
     * @param text The display text of the toast.
     * @param style The style of the toast.
     *
     * @return The created Toast instance.
     * @see Toast
     */
    fun createToast(
        icon: Material,
        text: Component,
        style: ToastStyle
    ): Toast

    /**
     * Sends a toast to the specified player.
     *
     * @param player The player to whom the toast will be sent.
     * @param toast The toast to be sent.
     *
     * @see Toast
     */
    fun sendToast(player: Player, toast: Toast)

    companion object {
        @JvmStatic
        val instance get() = SurfCoreApi.instance as SurfBukkitApi
    }
}

val surfBukkitApi get() = SurfBukkitApi.instance