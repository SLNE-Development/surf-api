package dev.slne.surf.api.paper.scoreboard

import org.bukkit.entity.Player

/**
 * SurfAutoUpdatablePlayerScoreboard is an interface representing an auto-updatable scoreboard that
 * includes all online players as viewers. This interface extends the SurfAutoUpdatableScoreboard
 * interface.
 */
interface SurfAutoUpdatablePlayerScoreboard : SurfAutoUpdatableScoreboard {
    /**
     * Adds a viewer to the scoreboard. In this implementation, this method does nothing and prints a
     * warning to the console because all online players are automatically added as viewers.
     *
     * @param viewer the player to add as a viewer
     */
    @Deprecated(
        message = "All online players are automatically added as viewers",
        level = DeprecationLevel.ERROR,
    )
    override fun addViewer(viewer: Player)

    /**
     * Removes a viewer from the scoreboard. In this implementation, this method does nothing and
     * prints a warning to the console because the scoreboard is always visible to all online
     * players.
     *
     * @param viewer the player to remove as a viewer
     */
    @Deprecated(
        message = "The scoreboard is always visible to all online players",
        level = DeprecationLevel.ERROR,
    )
    override fun removeViewer(viewer: Player)
}