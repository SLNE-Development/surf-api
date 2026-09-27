package dev.slne.surf.api.paper.server.impl.scoreboard

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.paper.extensions.server
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.common.NmsProvider
import dev.slne.surf.api.paper.nms.common.scoreboard.PlayerNmsScoreboard
import dev.slne.surf.api.paper.scoreboard.SidebarLine
import dev.slne.surf.api.paper.scoreboard.SurfScoreboard
import dev.slne.surf.api.paper.server.plugin
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer
import kotlin.concurrent.withLock

open class SurfScoreboardImpl(definition: ScoreboardDefinition) : SurfScoreboard {
    protected val lock = ReentrantLock()

    private val title = definition.title
    private val maxLines = definition.maxLines
    private val lines = definition.lines
    private val snapshots = definition.snapshots
    private val animations = definition.animations
    private val hasViewerLines = lines.any { it is ScoreboardLine.Viewer }

    private var enabled: Boolean = false

    private val sharedLines = arrayOfNulls<List<SidebarLine>>(lines.size)
    private val viewerBoards = Object2ObjectOpenHashMap<UUID, ViewerBoard>()

    override fun addViewer(viewer: Player) {
        lock.withLock {
            checkEnabled()
            addViewerInternal(viewer)
        }
    }

    /** Adds [viewer] without the public API checks of subclasses. */
    protected fun addViewerInternal(viewer: Player) {
        assert(lock.isHeldByCurrentThread) { "addViewerInternal must be called with lock held" }

        val existing = viewerBoards[viewer.uniqueId]
        if (existing != null) {
            if (existing.viewer === viewer) return
            existing.close()
        }

        val board = ViewerBoard(viewer)
        viewerBoards[viewer.uniqueId] = board
        render(board)
    }

    override fun removeViewer(viewer: Player) {
        lock.withLock {
            checkEnabled()
            viewerBoards.remove(viewer.uniqueId)?.close()
        }
    }

    override fun enable() {
        lock.withLock {
            check(!enabled) { "Scoreboard is already enabled" }

            snapshots.forEach { it.refresh() }
            refreshSharedLines()

            enabled = true
        }
    }

    override fun disable() {
        lock.withLock {
            checkEnabled()

            viewerBoards.values.forEach { it.close() }
            viewerBoards.clear()
            animations.forEach { it.reset() }

            enabled = false
        }
    }

    override fun update() {
        lock.withLock {
            checkEnabled()

            removeDisconnectedViewers()
            animations.forEach { it.nextFrame() }
            snapshots.forEach { it.refresh() }
            refreshSharedLines()
            viewerBoards.values.forEach(::render)

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

    private fun refreshSharedLines() {
        lines.forEachIndexed { index, line ->
            if (line is ScoreboardLine.Shared) {
                val drawn = ObjectArrayList<SidebarLine>().apply { line.draw(this) }
                val previous = sharedLines[index]
                if (previous != null) {
                    for (i in 0 until minOf(drawn.size, previous.size)) {
                        if (drawn[i] == previous[i]) drawn[i] = previous[i]
                    }
                }
                sharedLines[index] = drawn
            }
        }
    }

    private fun removeDisconnectedViewers() {
        assert(lock.isHeldByCurrentThread) { "removeDisconnectedViewers must be called with lock held" }

        viewerBoards.values.removeIf { board ->
            if (board.viewer.isConnected) return@removeIf false
            board.close()
            true
        }
    }

    private fun render(board: ViewerBoard) {
        if (hasViewerLines) scheduleRender(board) else board.show()
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

        val viewerLines = arrayOfNulls<List<SidebarLine>>(lines.size)
        lines.forEachIndexed { index, line ->
            if (line is ScoreboardLine.Viewer) {
                viewerLines[index] = ObjectImmutableList(line.factory.apply(board.viewer))
            }
        }

        lock.withLock {
            if (!enabled || viewerBoards[board.viewer.uniqueId] !== board) return

            board.viewerLines = viewerLines
            board.show()
        }
    }

    private fun checkEnabled() {
        check(enabled) { "Scoreboard is not enabled. Did you forget to call enable()?" }
    }

    private inner class ViewerBoard(val viewer: Player) {
        val renderPending = AtomicBoolean()
        val renderTask = Consumer<ScheduledTask> { renderViewer(this) }
        val retiredTask = Runnable { renderPending.set(false) }

        var viewerLines = arrayOfNulls<List<SidebarLine>>(lines.size)

        private val pendingContent = AtomicReference<List<SidebarLine>?>()
        private val applyScheduled = AtomicBoolean()
        private val applyTask = Runnable { drainPendingContent() }

        private var scoreboard: PlayerNmsScoreboard? = null
        private var closed = false

        fun show() {
            pendingContent.set(compose())
            if (applyScheduled.compareAndSet(false, true)) {
                APPLY_EXECUTOR.execute(applyTask)
            }
        }

        private fun drainPendingContent() {
            do {
                try {
                    pendingContent.getAndSet(null)?.let(::apply)
                } catch (e: Throwable) {
                    log.atWarning().withCause(e).log("Failed to update scoreboard of %s", viewer.name)
                } finally {
                    applyScheduled.set(false)
                }
            } while (pendingContent.get() != null && applyScheduled.compareAndSet(false, true))
        }

        private fun apply(content: List<SidebarLine>) {
            synchronized(this) {
                if (closed) return

                val scoreboard = scoreboard ?: createScoreboard().also { scoreboard = it }
                scoreboard.updateLines(content)
            }
        }

        private fun compose(): List<SidebarLine> {
            val content = ObjectArrayList<SidebarLine>(maxLines)
            for (index in lines.indices) {
                val part = sharedLines[index] ?: viewerLines[index] ?: continue
                for (line in part) {
                    if (content.size == maxLines) return content
                    content.add(line)
                }
            }
            return content
        }

        fun close() {
            synchronized(this) {
                closed = true
                pendingContent.set(null)
                scoreboard?.delete()
            }
        }

        @OptIn(NmsUseWithCaution::class)
        private fun createScoreboard() = NmsProvider.current.createPlayerScoreboard(viewer, title)

        override fun toString(): String {
            return "ViewerBoard(viewer=$viewer, scoreboard=$scoreboard, renderPending=$renderPending)"
        }
    }

    companion object {
        private val log = logger()

        private val APPLY_EXECUTOR = Dispatchers.Default.asExecutor()
    }
}
