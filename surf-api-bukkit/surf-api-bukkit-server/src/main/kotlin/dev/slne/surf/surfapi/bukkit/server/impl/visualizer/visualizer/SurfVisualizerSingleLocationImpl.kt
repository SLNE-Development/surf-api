@file:OptIn(NmsUseWithCaution::class)

package dev.slne.surf.surfapi.bukkit.server.impl.visualizer.visualizer

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
import dev.slne.surf.surfapi.bukkit.server.impl.visualizer.visualizerApiImpl
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.*

class SurfVisualizerSingleLocationImpl(location: Location) : AbstractSurfVisualizerImpl(),
    SurfVisualizerSingleLocation {
    private val entityId = nmsCommonBridge.nextEntityId()

    override var location: Location = location
        set(value) {
            ensureNotClosed()
            field = value
            update(UpdateStrategy.POSITION)
        }

    override var settings: BlockDisplaySettings = BlockDisplaySettings.create {
        blockData = SurfVisualizer.DEFAULT_BLOCK_TYPE.createBlockData()
    }
        set(value) {
            ensureNotClosed()
            field = value
            update()
        }

    init {
        cleaner.register(this, SingleLocationCleanupState(uid, entityId, internalViewerUuids))
    }

    private class SingleLocationCleanupState(
        private val uid: UUID,
        private val entityId: Int,
        private val viewerUuids: MutableSet<UUID>,
    ) : CleanupState() {
        override fun cleanup() {
            val despawn = nmsSpawnPackets.despawn(entityId)
            for (uuid in viewerUuids) {
                visualizerApiImpl.onViewerRemoved(uid, uuid)
                val player = Bukkit.getPlayer(uuid) ?: continue
                despawn.execute(player)
            }
            viewerUuids.clear()
        }
    }

    override fun onClose() {
    }

    override fun startVisualizingInternal() {
        update()
    }

    override fun stopVisualizingInternal() {
        for (uuid in viewerUuids) {
            val player = Bukkit.getPlayer(uuid) ?: continue
            despawnPacket().execute(player)
        }
    }

    override fun onPlayerReceiveChunk(player: Player, chunk: Chunk) {
        ensureNotClosed()
        if (chunk.world == location.world && location.chunkX == chunk.x && location.chunkZ == chunk.z) {
            spawnPacket().execute(player)
        }
    }

    override fun onPlayerUnloadChunk(player: Player, chunk: Chunk) {
        ensureNotClosed()
        if (chunk.world == location.world && location.chunkX == chunk.x && location.chunkZ == chunk.z) {
            despawnPacket().execute(player)
        }
    }

    override fun onViewerRemoved(player: Player) {
        ensureNotClosed()
        despawnPacket().execute(player)
    }

    override fun clearStaleData(uuid: UUID) {

    }

    override fun update(strategy: UpdateStrategy) {
        ensureNotClosed()
        if (!visualizing.get()) return

        when (strategy) {
            UpdateStrategy.ALL -> {
                val despawn = despawnPacket()
                val spawn = spawnPacket()

                for (viewer in viewerUuids) {
                    val player = Bukkit.getPlayer(viewer) ?: continue
                    player.enterContextIfNeeded {
                        despawn.execute(player)
                        if (!visualizing.get()) return@enterContextIfNeeded
                        val seesLocation = player.isChunkVisible(location)

                        if (seesLocation) {
                            spawn.execute(player)
                        }
                    }
                }
            }

            UpdateStrategy.POSITION -> {
                val updatePosition = updatePositionPacket()
                for (viewer in viewerUuids) {
                    val player = Bukkit.getPlayer(viewer) ?: continue
                    player.enterContextIfNeeded {
                        if (player.isChunkVisible(location)) {
                            if (!visualizing.get()) return@enterContextIfNeeded
                            updatePosition.execute(player)
                        } else {
                            despawnPacket().execute(player)
                        }
                    }
                }
            }
        }
    }

    override fun settings(consumer: BlockDisplaySettings.() -> Unit) {
        ensureNotClosed()
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