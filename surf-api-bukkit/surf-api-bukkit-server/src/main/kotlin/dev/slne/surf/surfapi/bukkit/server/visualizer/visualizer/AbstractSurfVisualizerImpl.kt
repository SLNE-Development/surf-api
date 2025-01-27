package dev.slne.surf.surfapi.bukkit.server.visualizer.visualizer

import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizer
import dev.slne.surf.surfapi.core.api.collection.TransformingObjectSet
import dev.slne.surf.surfapi.core.api.util.freeze
import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.core.api.util.mutableObjectSetOf
import dev.slne.surf.surfapi.core.api.util.synchronize
import org.bukkit.Bukkit
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
    override val viewers get() = _viewers.freeze()
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
            onViewerAdded(player)
        }
    }

    override fun removeViewer(player: Player) {
        if (viewerUuids.remove(player.uniqueId) && player.isOnline) {
            onViewerRemoved(player)
        }
    }

    override fun clearViewers() {
        viewerUuids.removeIf {
            val player = Bukkit.getPlayer(it)
            if (player?.isOnline == true) {
                onViewerRemoved(player)
            }
            true
        }
    }

    override fun hasViewers() = viewerUuids.isNotEmpty()
    override fun visibleTo(player: Player) = player.uniqueId in viewerUuids

    open fun onViewerAdded(player: Player) {
    }

    open fun onViewerRemoved(player: Player) {
    }

    protected abstract fun startVisualizingInternal()
    protected abstract fun stopVisualizingInternal()

}