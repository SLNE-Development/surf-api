@file:OptIn(NmsUseWithCaution::class)

package dev.slne.surf.surfapi.bukkit.server.visualizer.visualizer

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.nmsCommonBridge
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.BlockDisplaySettings
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.nmsSpawnPackets
import dev.slne.surf.surfapi.bukkit.api.util.chunkX
import dev.slne.surf.surfapi.bukkit.api.util.chunkZ
import dev.slne.surf.surfapi.bukkit.api.util.isChunkVisible
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizer
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizerSingleLocation
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.UpdateStrategy
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.entity.Player

class SurfVisualizerSingleLocationImpl(location: Location) : AbstractSurfVisualizerImpl(),
    SurfVisualizerSingleLocation {
    private val entityId = nmsCommonBridge.nextEntityId()

    override var location: Location = location
        set(value) {
            field = value
            update(UpdateStrategy.POSITION)
        }

    override var settings: BlockDisplaySettings = BlockDisplaySettings.create {
        blockData = SurfVisualizer.DEFAULT_BLOCK_TYPE.createBlockData()
    }
        set(value) {
            field = value
            update()
        }

    override fun startVisualizingInternal() {
        update()
    }

    override fun stopVisualizingInternal() {
        viewers.forEach { despawnPacket().execute(it) }
    }

    override fun onPlayerReceiveChunk(player: Player, chunk: Chunk) {
        if (chunk.world == location.world && location.chunkX == chunk.x && location.chunkZ == chunk.z) {
            spawnPacket().execute(player)
        }
    }

    override fun onPlayerUnloadChunk(player: Player, chunk: Chunk) {
        if (chunk.world == location.world && location.chunkX == chunk.x && location.chunkZ == chunk.z) {
            despawnPacket().execute(player)
        }
    }

    override fun update(strategy: UpdateStrategy) {
        if (!visualizing) return

        when (strategy) {
            UpdateStrategy.ALL -> {
                val despawn = despawnPacket()
                val spawn = spawnPacket()

                for (viewer in viewers) {
                    despawn.execute(viewer)
                    val seesLocation = viewer.isChunkVisible(location)

                    if (seesLocation) {
                        spawn.execute(viewer)
                    }
                }
            }

            UpdateStrategy.POSITION -> {
                val updatePosition = updatePositionPacket()
                for (viewer in viewers) {
                    if (viewer.isChunkVisible(location)) {
                        updatePosition.execute(viewer)
                    } else {
                        despawnPacket().execute(viewer)
                    }
                }
            }
        }
    }

    override fun settings(consumer: BlockDisplaySettings.() -> Unit) {
        settings.consumer()
        update()
    }

    private fun spawnPacket() = nmsSpawnPackets.spawnBlockDisplay(entityId, location, settings)
    private fun despawnPacket() = nmsSpawnPackets.despawn(entityId)
    private fun updatePositionPacket() =
        nmsSpawnPackets.teleport(entityId, location, location.yaw, location.pitch)

    override fun toString(): String {
        return "SurfVisualizerSingleLocationImpl(entityId=$entityId, location=$location, settings=$settings)"
    }
}