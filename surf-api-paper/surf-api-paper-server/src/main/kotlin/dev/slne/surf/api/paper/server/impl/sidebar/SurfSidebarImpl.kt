package dev.slne.surf.api.paper.server.impl.sidebar

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.core.util.runAtFixedRate
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

    private val sharedTitle = definition.title as? SidebarTitle.Shared
    private val sharedEntries = definition.entries.filterIsInstance<SidebarEntry.Shared>()
    private val hasSharedContent = sharedTitle != null || sharedEntries.isNotEmpty()
    private val sharedRefreshRequests = Channel<Unit>(Channel.CONFLATED)

    private val sharedReady = CompletableDeferred<Unit>()

    init {
        check(plugin.isEnabled) { "Plugin ${plugin.name} is not enabled" }
        SidebarRegistry.add(this)

        if (!hasSharedContent) {
            sharedReady.complete(Unit)
        } else {
            scope.launch {
                for (request in sharedRefreshRequests) {
                    refreshSharedContent()
                    sharedReady.complete(Unit)
                    sessions.values.forEach { it.requestRender() }
                }
            }
            sharedRefreshRequests.trySend(Unit)
        }
    }

    override val viewers: Collection<Player>
        get() = sessions.values.map { it.player }

    override val isClosed: Boolean
        get() = closed

    override fun update() {
        if (closed) return

        definition.animations.forEach { it.nextFrame() }
        if (!hasSharedContent) {
            sessions.values.forEach { it.requestRender() }
        } else {
            sharedRefreshRequests.trySend(Unit)
        }
    }

    private suspend fun refreshSharedContent() {
        if (sharedTitle != null) {
            val rendered = renderShared("title") { sharedTitle.render() }
            if (rendered != null && rendered != sharedTitle.current) {
                sharedTitle.current = rendered
            }
        }

        for (entry in sharedEntries) {
            val rendered = renderShared("line") { entry.render() } ?: continue
            val previous = entry.current
            entry.current = List(rendered.size) { index ->
                val line = rendered[index]
                val old = previous.getOrNull(index)
                if (old != null && old == line) old else line
            }
        }
    }

    private inline fun <T : Any> renderShared(content: String, render: () -> T): T? {
        return try {
            render()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.atWarning()
                .atMostEvery(10, TimeUnit.SECONDS)
                .withCause(e)
                .log("Failed to render shared sidebar %s of %s", content, plugin.name)
            null
        }
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

            sessions[player.uniqueId] = ViewerSession(player)
        }
    }

    protected fun removeViewerInternal(player: Player) {
        sessions.remove(player.uniqueId)?.close()
    }

    protected fun launchUpdater(interval: Duration) {
        scope.runAtFixedRate(interval, initialDelay = interval, taskName = "surf-sidebar-${plugin.name}") {
            update()
        }
    }

    @OptIn(NmsUseWithCaution::class)
    private inner class ViewerSession(val player: Player) {
        private val renderRequests = Channel<Unit>(Channel.CONFLATED)

        private val scoreboard: PlayerNmsScoreboard = NmsProvider.current.createPlayerScoreboard(player)
        private var closed = false

        private val renderJob = scope.launch {
            for (request in renderRequests) {
                render()
            }
        }

        init {
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

            sharedReady.await()

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
                if (!closed) scoreboard.update(title, lines)
            }
        }

        private suspend fun renderTitle(): Component = when (val title = definition.title) {
            is SidebarTitle.Static -> title.title
            is SidebarTitle.Rendered -> title.render(player)
            is SidebarTitle.Shared -> title.current
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

                    is SidebarEntry.Shared -> for (line in entry.current) {
                        if (lines.size == maxLines) break
                        lines.add(line)
                    }
                }
            }
            return lines
        }

        fun close() {
            renderJob.cancel()

            synchronized(this) {
                closed = true
                scoreboard.delete()
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
