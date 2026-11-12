package dev.slne.surf.api.paper.sidebar

import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import kotlin.time.Duration

/**
 * Represents a packet-based sidebar displayed to a collection of viewers.
 *
 * Titles and lines are rendered asynchronously on a background dispatcher for each viewer
 * whenever the sidebar is updated. Renderers may suspend. Code that requires access to the
 * viewer's region thread should switch to it using
 * `withContext(plugin.entityDispatcher(viewer))`.
 *
 * Rendering for the same viewer never overlaps. If additional updates are requested while a
 * render is already in progress, they are coalesced into a single follow-up render. Only content
 * that changed since the previous render is sent to the viewer.
 *
 * The sidebar is closed automatically when [plugin] is disabled, and viewers are removed
 * automatically when they leave the server.
 *
 * A player should only view one sidebar at a time, including legacy
 * [dev.slne.surf.api.paper.scoreboard.SurfScoreboard] instances, as they all use the same
 * sidebar display slot.
 *
 * All operations are thread-safe.
 */
interface SurfSidebar {

    /**
     * The plugin that owns this sidebar.
     */
    val plugin: Plugin

    /**
     * A snapshot of the players currently viewing this sidebar.
     */
    val viewers: Collection<Player>

    /**
     * Whether this sidebar has been closed.
     */
    val isClosed: Boolean

    /**
     * Requests a render for all current viewers and advances animated lines by one frame.
     *
     * This method returns immediately while rendering continues asynchronously. Multiple update
     * requests made while a viewer is already being rendered are coalesced into a single
     * follow-up render.
     *
     * Does nothing after the sidebar has been closed.
     */
    fun update()

    /**
     * Removes the sidebar from all viewers and stops further rendering.
     *
     * Subsequent calls have no effect.
     */
    fun close()
}

/**
 * A [SurfSidebar] whose viewers are managed explicitly by the caller.
 *
 * The sidebar is rendered when a viewer is added and subsequently whenever [update] is called.
 *
 * @see sidebar
 */
interface SurfViewerSidebar : SurfSidebar {

    /**
     * Adds [player] as a viewer and schedules an initial render for them.
     *
     * Does nothing if [player] is already viewing this sidebar. If a different [Player] instance
     * with the same UUID is added, such as after the player rejoins, it replaces the previous
     * instance.
     *
     * @param player the player to add
     * @throws IllegalStateException if this sidebar has already been closed
     */
    fun addViewer(player: Player)

    /**
     * Removes [player] as a viewer and clears the sidebar from their client.
     *
     * @param player the player to remove
     */
    fun removeViewer(player: Player)
}

/**
 * A [SurfViewerSidebar] that automatically calls [update] at a fixed interval.
 *
 * @see autoUpdatingSidebar
 */
interface SurfAutoUpdatingSidebar : SurfViewerSidebar {

    /**
     * The interval between automatic updates.
     */
    val updateInterval: Duration
}

/**
 * A [SurfSidebar] shown to every online player and to players who join later.
 *
 * The sidebar automatically calls [update] at a fixed [updateInterval].
 *
 * @see globalSidebar
 */
interface SurfGlobalSidebar : SurfSidebar {

    /**
     * The interval between automatic updates.
     */
    val updateInterval: Duration
}
