@file:OptIn(NmsUseWithCaution::class)

package dev.slne.surf.surfapi.bukkit.server.visualizer.visualizer

import dev.slne.surf.surfapi.bukkit.api.event.register
import dev.slne.surf.surfapi.bukkit.api.event.unregister
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.nmsCommonBridge
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.BlockDisplaySettings
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.nmsSpawnPackets
import dev.slne.surf.surfapi.bukkit.api.util.seesLocation
import dev.slne.surf.surfapi.bukkit.api.util.toPlayers
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizer
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizerSingleLocation
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.UpdateStrategy
import io.papermc.paper.event.packet.PlayerChunkLoadEvent
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class SurfVisualizerSingleLocationImpl(location: Location) : AbstractSurfVisualizerImpl(),
    SurfVisualizerSingleLocation {
    private var listener: VisualizerListener? = null
    private val entityId = nmsCommonBridge.nextEntityId()

    override var location: Location = location
        set(value) {
            field = value
            update(UpdateStrategy.POSITION)
        }

    override var settings: BlockDisplaySettings = BlockDisplaySettings().apply {
        blockData = SurfVisualizer.DEFAULT_MATERIAL.createBlockData()
    }
        set(value) {
            field = value
            update()
        }

    override fun startVisualizingInternal() {
        listener = VisualizerListener().also { it.register() }
        update()
    }

    override fun stopVisualizingInternal() {
        listener?.unregister()
        listener = null

        viewerUuids.toPlayers().forEach { despawnPacket().execute(it) }
    }

    override fun update(strategy: UpdateStrategy) {
        if (!visualizing) return

        when (strategy) {
            UpdateStrategy.ALL -> {
                val despawn = despawnPacket()
                val spawn = spawnPacket()

                for (viewer in viewerUuids.toPlayers()) {
                    despawn.execute(viewer)
                    val seesLocation = viewer.seesLocation(location)
                    println("seesLocation: $seesLocation")

                    if (seesLocation) {
                        spawn.execute(viewer)
                    }
                }
            }

            UpdateStrategy.POSITION -> {
                val updatePosition = updatePositionPacket()
                for (viewer in viewerUuids.toPlayers()) {
                    if (viewer.seesLocation(location)) {
                        updatePosition.execute(viewer)
                    }
                }
            }
        }
    }

    override fun settings(consumer: BlockDisplaySettings.() -> Unit) {
        settings.consumer()
        update()
    }

    override fun onViewerAdded(player: Player) {
        if (player.seesLocation(location)) {
            spawnPacket().execute(player)
        }
    }

    override fun onViewerRemoved(player: Player) {
        despawnPacket().execute(player)
    }

    private fun spawnPacket() = nmsSpawnPackets.spawnBlockDisplay(entityId, location, settings)
    private fun despawnPacket() = nmsSpawnPackets.despawn(entityId)
    private fun updatePositionPacket() =
        nmsSpawnPackets.teleport(entityId, location, location.yaw, location.pitch)

    private inner class VisualizerListener : Listener {
        @EventHandler
        fun onPlayerChunkLoad(event: PlayerChunkLoadEvent) {
            val player = event.player
            if (player.uniqueId !in viewerUuids) return
            if (player.world != location.world) return
            spawnPacket().execute(player)
        }
        @EventHandler
        fun onPlayerChunkUnload(event: PlayerChunkUnloadEvent) {
            val player = event.player
            if (player.uniqueId !in viewerUuids) return
            if (player.world != location.world) return
            despawnPacket().execute(player)
        }

        override fun toString(): String {
            return "VisualizerListener()"
        }
    }

    override fun toString(): String {
        return "SurfVisualizerSingleLocationImpl(entityId=$entityId, listener=$listener, location=$location, settings=$settings)"
    }
}