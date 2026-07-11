@file:OptIn(NmsUseWithCaution::class, ExperimentalVisualizerApi::class)

package dev.slne.surf.api.paper.server.impl.visualizer.visualizer

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsCommonBridge
import dev.slne.surf.api.paper.nms.bridges.packets.entity.BlockDisplaySettings
import dev.slne.surf.api.paper.nms.bridges.packets.entity.SurfPaperNmsSpawnPackets
import dev.slne.surf.api.paper.server.impl.visualizer.SurfBukkitVisualizerApiImpl
import dev.slne.surf.api.paper.util.chunkX
import dev.slne.surf.api.paper.util.chunkZ
import dev.slne.surf.api.paper.util.isChunkVisible
import dev.slne.surf.api.paper.visualizer.visualizer.ExperimentalVisualizerApi
import dev.slne.surf.api.paper.visualizer.visualizer.SurfVisualizer
import dev.slne.surf.api.paper.visualizer.visualizer.SurfVisualizerSingleLocation
import dev.slne.surf.api.paper.visualizer.visualizer.UpdateStrategy
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class SurfVisualizerSingleLocationImpl(
    location: Location
) : AbstractSurfVisualizerImpl(), SurfVisualizerSingleLocation {
    private val entityId = SurfPaperNmsCommonBridge.nextEntityId()
    private val sentViewerUuids = ConcurrentHashMap.newKeySet<UUID>()

    override var location: Location = location
        set(value) {
            ensureNotClosed()
            val worldChanged = field.world != value.world
            field = value
            update(if (worldChanged) UpdateStrategy.ALL else UpdateStrategy.POSITION)
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
        cleaner.register(
            this,
            SingleLocationCleanupState(
                uid,
                entityId,
                internalViewerUuids,
                sentViewerUuids
            )
        )
    }

    private class SingleLocationCleanupState(
        private val uid: UUID,
        private val entityId: Int,
        private val viewerUuids: MutableSet<UUID>,
        private val sentViewerUuids: MutableSet<UUID>,
    ) : CleanupState() {
        override fun cleanup() {
            val despawn = SurfPaperNmsSpawnPackets.despawn(entityId)
            for (uuid in viewerUuids) {
                SurfBukkitVisualizerApiImpl.INSTANCE.onViewerRemoved(uid, uuid)
                if (sentViewerUuids.remove(uuid)) {
                    val player = Bukkit.getPlayer(uuid) ?: continue
                    despawn.execute(player)
                }
            }
            viewerUuids.clear()
            sentViewerUuids.clear()
        }
    }

    override fun onClose() {
        sentViewerUuids.clear()
    }

    override fun startVisualizingInternal() {
        update()
    }

    override fun stopVisualizingInternal() {
        val iterator = sentViewerUuids.iterator()
        while (iterator.hasNext()) {
            val uuid = iterator.next()
            iterator.remove()
            val player = Bukkit.getPlayer(uuid) ?: continue
            player.enterContextIfNeeded {
                despawnPacket().execute(player)
            }
        }
    }

    override fun onViewerAdded(player: Player) {
        ensureNotClosed()
        if (!visualizing.get()) return

        val version = currentStateVersion()
        val locationSnapshot = location.clone()
        val spawn = spawnPacket(locationSnapshot, settings.clone())
        player.enterContextIfNeeded {
            if (!isActiveVersion(version)) return@enterContextIfNeeded
            if (player.isChunkVisible(locationSnapshot) && sentViewerUuids.add(player.uniqueId)) {
                spawn.execute(player)
            }
        }
    }

    override fun onPlayerReceiveChunk(player: Player, chunk: Chunk) {
        ensureNotClosed()
        if (!visualizing.get()) return
        val locationSnapshot = location
        if (chunk.world == locationSnapshot.world &&
            locationSnapshot.chunkX == chunk.x &&
            locationSnapshot.chunkZ == chunk.z &&
            sentViewerUuids.add(player.uniqueId)
        ) {
            spawnPacket(locationSnapshot, settings.clone()).execute(player)
        }
    }

    override fun onPlayerUnloadChunk(player: Player, chunk: Chunk) {
        ensureNotClosed()
        val locationSnapshot = location
        if (chunk.world == locationSnapshot.world &&
            locationSnapshot.chunkX == chunk.x &&
            locationSnapshot.chunkZ == chunk.z &&
            sentViewerUuids.remove(player.uniqueId)
        ) {
            despawnPacket().execute(player)
        }
    }

    override fun onViewerRemoved(player: Player) {
        ensureNotClosed()
        if (sentViewerUuids.remove(player.uniqueId)) {
            player.enterContextIfNeeded {
                despawnPacket().execute(player)
            }
        }
    }

    override fun clearStaleData(uuid: UUID) {
        sentViewerUuids.remove(uuid)
    }

    override fun update(strategy: UpdateStrategy) {
        ensureNotClosed()
        if (!visualizing.get()) return

        val version = currentStateVersion()
        val locationSnapshot = location.clone()
        when (strategy) {
            UpdateStrategy.ALL -> {
                val despawn = despawnPacket()
                val spawn = spawnPacket(locationSnapshot, settings.clone())

                for (viewer in viewerUuids) {
                    val player = Bukkit.getPlayer(viewer) ?: continue
                    player.enterContextIfNeeded {
                        if (!isActiveVersion(version)) return@enterContextIfNeeded
                        if (sentViewerUuids.remove(viewer)) {
                            despawn.execute(player)
                        }

                        if (player.isChunkVisible(locationSnapshot) && sentViewerUuids.add(viewer)) {
                            spawn.execute(player)
                        }
                    }
                }
            }

            UpdateStrategy.POSITION -> {
                val updatePosition = updatePositionPacket(locationSnapshot)
                val spawn = spawnPacket(locationSnapshot, settings.clone())
                val despawn = despawnPacket()
                for (viewer in viewerUuids) {
                    val player = Bukkit.getPlayer(viewer) ?: continue
                    player.enterContextIfNeeded {
                        if (!isActiveVersion(version)) return@enterContextIfNeeded
                        if (player.isChunkVisible(locationSnapshot)) {
                            if (sentViewerUuids.add(viewer)) {
                                spawn.execute(player)
                            } else {
                                updatePosition.execute(player)
                            }
                        } else if (sentViewerUuids.remove(viewer)) {
                            despawn.execute(player)
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

    private fun spawnPacket(
        location: Location = this.location,
        settings: BlockDisplaySettings = this.settings,
    ) = SurfPaperNmsSpawnPackets.spawnBlockDisplay(entityId, location, settings)

    private fun despawnPacket() = SurfPaperNmsSpawnPackets.despawn(entityId)
    private fun updatePositionPacket(location: Location = this.location) =
        SurfPaperNmsSpawnPackets.teleport(entityId, location, location.yaw, location.pitch)

    override fun toString(): String {
        return "SurfVisualizerSingleLocationImpl(entityId=$entityId, location=$location, settings=$settings)"
    }
}
