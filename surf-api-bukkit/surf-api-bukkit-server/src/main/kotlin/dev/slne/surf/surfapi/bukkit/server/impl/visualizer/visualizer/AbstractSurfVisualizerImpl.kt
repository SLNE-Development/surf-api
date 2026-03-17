package dev.slne.surf.surfapi.bukkit.server.impl.visualizer.visualizer

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizer
import dev.slne.surf.surfapi.bukkit.server.plugin
import dev.slne.surf.surfapi.core.api.util.logger
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.entity.Player
import java.lang.ref.Cleaner
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

abstract class AbstractSurfVisualizerImpl : SurfVisualizer {
    protected val log = logger()

    companion object {
        private val cleaner = Cleaner.create()
    }

    protected val viewerUuids: MutableSet<UUID> = ConcurrentHashMap.newKeySet()
    override val viewers: Set<UUID> = Collections.unmodifiableSet(viewerUuids)
    override val uid: UUID = UUID.randomUUID()

    protected val visualizing = AtomicBoolean(false)

    init {
        val cleanupState = createCleanupState()
        cleaner.register(this, cleanupState)
    }

    protected abstract fun createCleanupState(): CleanupState

    abstract class CleanupState : Runnable {
        private val log = logger()

        override fun run() {
            try {
                cleanup()
            } catch (e: Throwable) {
                log.atWarning()
                    .withCause(e)
                    .log("Failed to clean up visualizer on GC")
            }
        }

        protected abstract fun cleanup()
    }

    override fun startVisualizing(): Boolean {
        if (!visualizing.compareAndSet(false, true)) return false

        try {
            startVisualizingInternal()
            return true
        } catch (e: Throwable) {
            visualizing.set(false)
            log.atSevere()
                .withCause(e)
                .log("Failed to start visualizing")
            return false
        }
    }

    override fun stopVisualizing(): Boolean {
        if (!visualizing.compareAndSet(true, false)) return false

        try {
            stopVisualizingInternal()
            return true
        } catch (e: Throwable) {
            visualizing.set(true)
            log.atSevere()
                .withCause(e)
                .log("Failed to stop visualizing")
            return false
        }
    }


    override fun isVisualizing() = visualizing.get()

    override fun addViewer(player: Player) {
        if (player.isOnline && viewerUuids.add(player.uniqueId)) {
            onViewerAdded(player)
        }
    }

    override fun removeViewer(player: Player) {
        if (viewerUuids.remove(player.uniqueId)) {
            if (player.isOnline) {
                onViewerRemoved(player)
            }
        }
    }

    override fun clearViewers() {
        val iterator = viewerUuids.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            Bukkit.getPlayer(next)?.let { onViewerRemoved(it) }
            iterator.remove()
        }
    }

    override fun hasViewers() = viewerUuids.isNotEmpty()
    override fun visibleTo(player: Player) = player.uniqueId in viewerUuids

    open fun onViewerAdded(player: Player) {
        if (!visualizing.get()) return

        player.enterContextIfNeeded {
            player.sentChunks.forEach { chunk ->
                onPlayerReceiveChunk(player, chunk)
            }
        }
    }

    open fun onViewerRemoved(player: Player) {
        if (!visualizing.get()) return

        player.enterContextIfNeeded {
            player.sentChunks.forEach { chunk ->
                onPlayerUnloadChunk(player, chunk)
            }
        }
    }

    protected abstract fun startVisualizingInternal()
    protected abstract fun stopVisualizingInternal()

    abstract fun onPlayerReceiveChunk(player: Player, chunk: Chunk)
    abstract fun onPlayerUnloadChunk(player: Player, chunk: Chunk)

    protected inline fun Player.enterContextIfNeeded(crossinline action: () -> Unit) {
        if (server.isOwnedByCurrentRegion(this)) {
            action()
        } else {
            plugin.launch(plugin.entityDispatcher(this)) {
                action()
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractSurfVisualizerImpl) return false

        if (uid != other.uid) return false

        return true
    }

    override fun hashCode(): Int {
        return uid.hashCode()
    }
}