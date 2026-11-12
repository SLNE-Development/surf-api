package dev.slne.surf.api.paper.scoreboard

import org.bukkit.entity.Player

/**
 * The SurfScoreboard interface represents a scoreboard in a surf game. This scoreboard can be
 * enabled, disabled, and updated. Viewers can be added and removed from the scoreboard.
 *
 * All methods are thread-safe. Viewers that disconnect are removed on the next [update].
 */
@Deprecated(
    "Replaced by the sidebar API (dev.slne.surf.api.paper.sidebar). " +
        "Create a sidebar with sidebar(plugin) { ... }; it returns a SurfViewerSidebar with the same " +
        "addViewer, removeViewer and update methods. enable() is no longer needed because a sidebar " +
        "is active once created, and close() replaces disable().",
    ReplaceWith("SurfViewerSidebar", "dev.slne.surf.api.paper.sidebar.SurfViewerSidebar")
)
interface SurfScoreboard {
    /**
     * Adds a viewer to the scoreboard.
     *
     * @param viewer the player to add as a viewer
     */
    fun addViewer(viewer: Player)

    /**
     * Removes a viewer from the scoreboard. The viewer will no longer see updates on the scoreboard
     *
     * @param viewer the player to remove as a viewer
     */
    fun removeViewer(viewer: Player)

    /**
     * Creates the scoreboard. This method must be called before any viewers are added.
     *
     * @throws IllegalStateException if the scoreboard is already enabled
     */
    fun enable()

    /**
     * Disables the SurfScoreboard. This method  closes the scoreboard. It also resets the animations
     * and the enabled flag.
     *
     * @throws IllegalStateException if the scoreboard is not enabled
     */
    fun disable()

    /**
     * Updates the scoreboard. This method should be called periodically to update the contents of the
     * scoreboard.
     *
     *
     * This method is responsible for updating any animations and applying the updated layout to the
     * scoreboard. It should only be called when the scoreboard is enabled.
     *
     * @throws IllegalStateException if the scoreboard is not enabled
     */
    fun update()
}