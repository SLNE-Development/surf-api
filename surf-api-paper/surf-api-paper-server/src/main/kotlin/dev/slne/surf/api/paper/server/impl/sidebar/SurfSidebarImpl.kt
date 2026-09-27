package dev.slne.surf.api.paper.server.impl.sidebar

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.common.NmsProvider
import dev.slne.surf.api.paper.nms.common.scoreboard.PlayerNmsScoreboard
import dev.slne.surf.api.paper.sidebar.*
import dev.slne.surf.api.paper.util.forEachPlayer
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

abstract class SurfSidebarImpl(
    final override val plugin: Plugin,
    private val definition: SidebarDefinition,
) : SurfSidebar {
    private val sessions = ConcurrentHashMap<UUID, ViewerSession>()
    private val lifecycleLock = Any()

    @Volatile
    private var closed = false

    protected val scope = CoroutineScope(
        SupervisorJob() +
                Dispatchers.Default +
                CoroutineName("surf-sidebar-${plugin.name}") +
                CoroutineExceptionHandler { _, e ->
                    log.atWarning()
                        .withCause(e)
                        .log("Uncaught exception in sidebar of %s", plugin.name)
                }
    )

    init {
        check(plugin.isEnabled) { "Plugin ${plugin.name} is not enabled" }
        SidebarRegistry.add(this)
    }

    override val viewers: Collection<Player>
        get() = sessions.values.map { it.player }

    override val isClosed: Boolean
        get() = closed

    override fun update() {
        if (closed) return

        definition.animations.forEach { it.nextFrame() }
        sessions.values.forEach { it.requestRender() }
    }

    override fun close() {
        synchronized(lifecycleLock) {
            if (closed) return
            closed = true
        }

        SidebarRegistry.remove(this)
        scope.cancel()
        sessions.values.forEach { it.close() }
        sessions.clear()
    }

    open fun onPlayerJoin(player: Player) {
    }

    fun onPlayerQuit(player: Player) {
        val session = sessions[player.uniqueId] ?: return
        if (session.player === player && sessions.remove(player.uniqueId, session)) {
            session.close()
        }
    }

    protected fun addViewerInternal(player: Player) {
        synchronized(lifecycleLock) {
            check(!closed) { "Sidebar is closed" }

            val existing = sessions[player.uniqueId]
            if (existing != null) {
                if (existing.player === player) return
                existing.close()
            }

            val session = ViewerSession(player)
            sessions[player.uniqueId] = session
            session.start()
        }
    }

    protected fun removeViewerInternal(player: Player) {
        sessions.remove(player.uniqueId)?.close()
    }

    protected fun launchUpdater(interval: Duration) {
        scope.launch {
            while (isActive) {
                delay(interval)
                update()
            }
        }
    }

    private inner class ViewerSession(val player: Player) {
        private val renderRequests = Channel<Unit>(Channel.CONFLATED)
        private lateinit var renderJob: Job

        private var scoreboard: PlayerNmsScoreboard? = null
        private var closed = false

        fun start() {
            renderJob = scope.launch {
                for (request in renderRequests) {
                    render()
                }
            }
            requestRender()
        }

        fun requestRender() {
            renderRequests.trySend(Unit)
        }

        private suspend fun render() {
            if (!player.isConnected) {
                if (sessions.remove(player.uniqueId, this)) close()
                return
            }

            val title: Component
            val lines: List<SidebarLine>
            try {
                title = renderTitle()
                lines = renderLines()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.atWarning()
                    .atMostEvery(10, TimeUnit.SECONDS)
                    .withCause(e)
                    .log("Failed to render sidebar of %s for %s", plugin.name, player.name)
                return
            }

            synchronized(this) {
                if (closed) return

                val scoreboard = scoreboard
                if (scoreboard == null) {
                    this.scoreboard = createScoreboard(title).also { it.updateLines(lines) }
                } else {
                    scoreboard.updateTitle(title)
                    scoreboard.updateLines(lines)
                }
            }
        }

        private suspend fun renderTitle(): Component = when (val title = definition.title) {
            is SidebarTitle.Static -> title.title
            is SidebarTitle.Rendered -> title.render(player)
        }

        private suspend fun renderLines(): List<SidebarLine> {
            val maxLines = definition.maxLines
            val lines = ObjectArrayList<SidebarLine>(maxLines)
            for (entry in definition.entries) {
                if (lines.size == maxLines) break

                when (entry) {
                    is SidebarEntry.Static -> lines.add(entry.line)
                    is SidebarEntry.Line -> lines.add(entry.render(player))
                    is SidebarEntry.Animated -> lines.add(entry.animation.currentFrame)
                    is SidebarEntry.Lines -> for (line in entry.render(player)) {
                        if (lines.size == maxLines) break
                        lines.add(line)
                    }
                }
            }
            return lines
        }

        @OptIn(NmsUseWithCaution::class)
        private fun createScoreboard(title: Component): PlayerNmsScoreboard {
            return NmsProvider.current.createPlayerScoreboard(player, title)
        }

        fun close() {
            renderRequests.close()
            if (::renderJob.isInitialized) renderJob.cancel()

            synchronized(this) {
                closed = true
                scoreboard?.delete()
            }
        }
    }

    companion object {
        private val log = logger()
    }
}

open class SurfViewerSidebarImpl(
    plugin: Plugin,
    definition: SidebarDefinition,
) : SurfSidebarImpl(plugin, definition), SurfViewerSidebar {

    override fun addViewer(player: Player) {
        addViewerInternal(player)
    }

    override fun removeViewer(player: Player) {
        removeViewerInternal(player)
    }
}

class SurfAutoUpdatingSidebarImpl(
    plugin: Plugin,
    definition: SidebarDefinition,
    override val updateInterval: Duration,
) : SurfViewerSidebarImpl(plugin, definition), SurfAutoUpdatingSidebar {
    init {
        launchUpdater(updateInterval)
    }
}

class SurfGlobalSidebarImpl(
    plugin: Plugin,
    definition: SidebarDefinition,
    override val updateInterval: Duration,
) : SurfSidebarImpl(plugin, definition), SurfGlobalSidebar {
    init {
        forEachPlayer(::addViewerInternal)
        launchUpdater(updateInterval)
    }

    override fun onPlayerJoin(player: Player) {
        addViewerInternal(player)
    }
}
