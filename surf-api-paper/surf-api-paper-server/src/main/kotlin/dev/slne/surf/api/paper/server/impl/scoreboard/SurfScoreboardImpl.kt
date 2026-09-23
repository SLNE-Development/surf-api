package dev.slne.surf.api.paper.server.impl.scoreboard

import dev.slne.surf.api.core.util.mutableObject2ObjectMapOf
import dev.slne.surf.api.paper.extensions.server
import dev.slne.surf.api.paper.scoreboard.SurfScoreboard
import dev.slne.surf.api.paper.scoreboard.SurfScoreboardApi
import dev.slne.surf.api.paper.server.plugin
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import net.kyori.adventure.text.Component
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar
import net.megavex.scoreboardlibrary.api.sidebar.component.ComponentSidebarLayout
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent
import net.megavex.scoreboardlibrary.api.sidebar.component.animation.FramedSidebarAnimation
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer
import kotlin.concurrent.withLock

open class SurfScoreboardImpl(definition: ScoreboardDefinition) : SurfScoreboard {
    protected val lock = ReentrantLock()

    private val maxLines = definition.maxLines
    private val lines = definition.lines
    private val snapshots = definition.snapshots
    private val animations = definition.animations
    private val titleComponent = SidebarComponent.staticLine(definition.title)

    /** Layout of the single shared sidebar, or `null` if the scoreboard contains viewer lines. */
    private val sharedLayout = if (lines.all { it is ScoreboardLine.Shared }) {
        layoutOf(lines.map { (it as ScoreboardLine.Shared).component })
    } else {
        null
    }
    private val perViewer = sharedLayout == null

    private var enabled: Boolean = false
    private var sharedSidebar: Sidebar? = null
    private val viewerBoards = mutableObject2ObjectMapOf<UUID, ViewerBoard>()

    override fun addViewer(viewer: Player) {
        lock.withLock {
            checkEnabled()
            addViewerInternal(viewer)
        }
    }

    /** Adds [viewer] without the public API checks of subclasses. */
    protected fun addViewerInternal(viewer: Player) {
        assert(lock.isHeldByCurrentThread) { "addViewerInternal must be called with lock held" }

        if (!perViewer) {
            sharedSidebar!!.addPlayer(viewer)
            return
        }

        val existing = viewerBoards[viewer.uniqueId]
        if (existing != null) {
            if (existing.viewer === viewer) return
            existing.sidebar.close()
        }

        val board = ViewerBoard(viewer, createSidebar())
        viewerBoards[viewer.uniqueId] = board
        scheduleRender(board)
    }

    override fun removeViewer(viewer: Player) {
        lock.withLock {
            checkEnabled()

            if (perViewer) {
                viewerBoards.remove(viewer.uniqueId)?.sidebar?.close()
            } else {
                sharedSidebar!!.removePlayer(viewer)
            }
        }
    }

    override fun enable() {
        lock.withLock {
            check(!enabled) { "Scoreboard is already enabled" }

            snapshots.forEach { it.refresh() }
            if (sharedLayout != null) {
                sharedSidebar = createSidebar().also { sharedLayout.apply(it) }
            }

            enabled = true
        }
    }

    override fun disable() {
        lock.withLock {
            checkEnabled()

            sharedSidebar?.close()
            sharedSidebar = null
            viewerBoards.values.forEach { it.sidebar.close() }
            viewerBoards.clear()
            animations.forEach { (it as? FramedSidebarAnimation<Component>)?.switchFrame(0) }

            enabled = false
        }
    }

    override fun update() {
        lock.withLock {
            checkEnabled()

            removeDisconnectedViewers()
            animations.forEach { it.nextFrame() }
            snapshots.forEach { it.refresh() }

            if (sharedLayout != null) {
                sharedLayout.apply(sharedSidebar!!)
            } else {
                viewerBoards.values.forEach(::scheduleRender)
            }

            onUpdate()
        }
    }

    /** Runs [update] if the scoreboard is enabled; does nothing otherwise. */
    protected fun updateIfEnabled() {
        lock.withLock {
            if (enabled) update()
        }
    }

    /** Called at the end of every [update] while [lock] is held. */
    protected open fun onUpdate() {}

    private fun removeDisconnectedViewers() {
        assert(lock.isHeldByCurrentThread) { "removeDisconnectedViewers must be called with lock held" }

        if (perViewer) {
            viewerBoards.values.removeIf { board ->
                if (board.viewer.isConnected) return@removeIf false
                board.sidebar.close()
                true
            }
        } else {
            val sidebar = sharedSidebar!!
            sidebar.players().filterNot { it.isConnected }.forEach(sidebar::removePlayer)
        }
    }

    private fun scheduleRender(board: ViewerBoard) {
        if (!board.renderPending.compareAndSet(false, true)) return

        if (board.viewer.scheduler.run(plugin, board.renderTask, board.retiredTask) == null) {
            board.renderPending.set(false)
        }
    }

    private fun renderViewer(board: ViewerBoard) {
        assert(server.isOwnedByCurrentRegion(board.viewer)) { "renderViewer must be called on the viewer's entity scheduler thread" }

        board.renderPending.set(false)

        val components = arrayOfNulls<SidebarComponent>(lines.size)
        lines.forEachIndexed { index, line ->
            if (line is ScoreboardLine.Viewer) {
                components[index] = line.factory.apply(board.viewer)
            }
        }

        lock.withLock {
            if (!enabled || viewerBoards[board.viewer.uniqueId] !== board) return

            board.viewerComponents = components
            board.layout.apply(board.sidebar)
            board.sidebar.addPlayer(board.viewer)
        }
    }

    private fun layoutOf(components: List<SidebarComponent>) = ComponentSidebarLayout(
        titleComponent,
        SidebarComponent.builder().apply { components.forEach(::addComponent) }.build()
    )

    private fun createSidebar() = SurfScoreboardApi.scoreboardLibrary().createSidebar(maxLines)

    private fun checkEnabled() {
        check(enabled) { "Scoreboard is not enabled. Did you forget to call enable()?" }
    }

    private inner class ViewerBoard(val viewer: Player, val sidebar: Sidebar) {
        val renderPending = AtomicBoolean()
        val renderTask = Consumer<ScheduledTask> { renderViewer(this) }
        val retiredTask = Runnable { renderPending.set(false) }

        /** Components of the viewer lines from the last render, indexed like [lines]. */
        var viewerComponents = arrayOfNulls<SidebarComponent>(lines.size)

        val layout = layoutOf(lines.mapIndexed { index, line ->
            when (line) {
                is ScoreboardLine.Shared -> line.component
                is ScoreboardLine.Viewer -> SidebarComponent { drawable ->
                    viewerComponents[index]?.draw(drawable)
                }
            }
        })

        override fun toString(): String {
            return "ViewerBoard(viewer=$viewer, sidebar=$sidebar, renderPending=$renderPending)"
        }
    }
}
