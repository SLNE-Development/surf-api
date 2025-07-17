package dev.slne.surf.surfapi.bukkit.server.impl.visualizer.visualizer

import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizer
import dev.slne.surf.surfapi.core.api.collection.TransformingObjectSet
import dev.slne.surf.surfapi.core.api.util.freeze
import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.core.api.util.mutableObjectSetOf
import dev.slne.surf.surfapi.core.api.util.synchronize
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.entity.Player
import java.lang.ref.Cleaner
import java.util.*

abstract class AbstractSurfVisualizerImpl : SurfVisualizer {
    protected val log = logger()

    companion object {
        private val cleaner = Cleaner.create()
    }

    protected val viewerUuids = mutableObjectSetOf<UUID>().synchronize()
    private val _viewers =
        TransformingObjectSet(viewerUuids, { Bukkit.getPlayer(it) }, { it.uniqueId })
    override val viewers  = _viewers.freeze()
    override val uid: UUID = UUID.randomUUID()

    @Volatile
    protected var visualizing = false

    init {
        cleaner.register(this) {
            stopVisualizing()
        }
    }

    override fun startVisualizing(): Boolean {
        if (visualizing) return false

        try {
            visualizing = true
            startVisualizingInternal()
            return true
        } catch (e: Throwable) {
            log.atSevere()
                .withCause(e)
                .log("Failed to start visualizing")
            return false
        }
    }

    override fun stopVisualizing(): Boolean {
        if (!visualizing) return false

        try {
            stopVisualizingInternal()
            visualizing = false
            return true
        } catch (e: Throwable) {
            log.atSevere()
                .withCause(e)
                .log("Failed to stop visualizing")
            return false
        }
    }


    override fun isVisualizing() = visualizing

    override fun addViewer(player: Player) {
        if (player.isOnline && viewerUuids.add(player.uniqueId)) {
            VisualizerManager.addVisualizer(player.uniqueId, this)
            onViewerAdded(player)
        }
    }

    override fun removeViewer(player: Player) {
        if (viewerUuids.remove(player.uniqueId)) {
            VisualizerManager.removeVisualizer(player.uniqueId, this)
            if (player.isOnline) {
                onViewerRemoved(player)
            }
        }
    }

    override fun clearViewers() {
        for (uuid in viewerUuids) {
            VisualizerManager.removeVisualizer(uuid, this)
            Bukkit.getPlayer(uuid)
                ?.takeIf { it.isOnline }
                ?.let { onViewerRemoved(it) }
        }

        viewerUuids.clear()
    }

    override fun hasViewers() = viewerUuids.isNotEmpty()
    override fun visibleTo(player: Player) = player.uniqueId in viewerUuids

    open fun onViewerAdded(player: Player) {
        if (!visualizing) return
        player.sentChunks.forEach { chunk ->
            onPlayerReceiveChunk(player, chunk)
        }
    }

    open fun onViewerRemoved(player: Player) {
        if (!visualizing) return
        player.sentChunks.forEach { chunk ->
            onPlayerUnloadChunk(player, chunk)
        }
    }

    protected abstract fun startVisualizingInternal()
    protected abstract fun stopVisualizingInternal()

    abstract fun onPlayerReceiveChunk(player: Player, chunk: Chunk)
    abstract fun onPlayerUnloadChunk(player: Player, chunk: Chunk)

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