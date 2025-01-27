@file:OptIn(NmsUseWithCaution::class)

package dev.slne.surf.surfapi.bukkit.server.visualizer.visualizer

import dev.slne.surf.surfapi.bukkit.api.event.register
import dev.slne.surf.surfapi.bukkit.api.event.unregister
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.nmsCommonBridge
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.BlockDisplaySettings
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.nmsSpawnPackets
import dev.slne.surf.surfapi.bukkit.api.util.seesLocation
import dev.slne.surf.surfapi.bukkit.api.util.toPlayers
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizerMultipleLocations
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.UpdateStrategy
import dev.slne.surf.surfapi.core.api.util.*
import io.papermc.paper.event.packet.PlayerChunkLoadEvent
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent
import it.unimi.dsi.fastutil.ints.IntSet
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class SurfVisualizerMultipleLocationsImpl : AbstractSurfVisualizerImpl(),
    SurfVisualizerMultipleLocations {

    private var listener: VisualizerListener? = null
    private val id2point = mutableInt2ObjectMapOf<VisualPoint>()
    private val point2Id =
        mutableObject2IntMapOf<VisualPoint>().apply { defaultReturnValue(Int.MIN_VALUE) }
    private val sentToPlayers = mutableObject2ObjectMapOf<Player, IntSet>()
    override val visualLocations
        get() = id2point.mapTo(mutableObjectSetOf()) { it.value.location }.freeze()

    override fun startVisualizingInternal() {
        listener = VisualizerListener().also { it.register() }
        update()
    }

    override fun stopVisualizingInternal() {
        listener?.unregister()
        listener = null

        for (viewer in viewerUuids.toPlayers()) {
            val despawn = PacketOperation.start()
            val sent = getSentToPlayer(viewer)
            for (id in sent.intIterator()) {
                despawn + nmsSpawnPackets.despawn(id)
            }
        }
    }

    override fun update(strategy: UpdateStrategy) {
        if (!visualizing) return

        when (strategy) {
            UpdateStrategy.ALL -> {
                for (viewer in viewerUuids.toPlayers()) {
                    val despawn = PacketOperation.start()
                    val spawn = PacketOperation.start()
                    val sent = getSentToPlayer(viewer)
                    val toDespawn = mutableIntSetOf()
                    val toSpawn = mutableIntSetOf()

                    for (id in sent.intIterator()) {
                        val point = id2point[id] ?: continue
                        if (!viewer.seesLocation(point.location)) {
                            toDespawn.add(id)
                            despawn + nmsSpawnPackets.despawn(id)
                        }
                    }

                    for (point in id2point.values) {
                        val id = point2Id.getInt(point)
                        if (viewer.seesLocation(point.location)) {
                            toSpawn.add(id)
                            spawn + spawnPacket(id, point)
                        }
                    }

                    if (!(despawn.isEmpty())) {
                        despawn.execute(viewer)
                        sent.removeAll(toDespawn)
                    }

                    if (!(spawn.isEmpty())) {
                        spawn.execute(viewer)
                        sent.addAll(toSpawn)
                    }
                }
            }

            UpdateStrategy.POSITION -> {
                for (viewer in viewerUuids.toPlayers()) {
                    val updatePosition = PacketOperation.start()
                    val sent = getSentToPlayer(viewer)

                    for (id in sent.intIterator()) {
                        val point = id2point[id] ?: continue
                        if (viewer.seesLocation(point.location)) {
                            updatePosition + updatePositionPacket(id, point)
                        }
                    }

                    if (!(updatePosition.isEmpty())) {
                        updatePosition.execute(viewer)
                    }
                }
            }
        }
    }

    override fun addVisualLocation(
        visualLocation: Location,
        settings: BlockDisplaySettings,
    ) {
        val id = nmsCommonBridge.nextEntityId()
        val point = VisualPoint(visualLocation, settings)
        put(id, point)

        if (!visualizing) return

        for (viewer in viewerUuids.toPlayers()) {
            spawn(viewer, id, point)
        }
    }

    override fun removeVisualLocation(visualLocation: Location) {
        val result = remove(visualLocation) ?: return
        val (id, point) = result

        if (!visualizing) return

        for (viewer in viewerUuids.toPlayers()) {
            despawn(viewer, id, point, true)
        }
    }

    override fun clearVisualLocations() {
        if (visualizing) {
            for (viewer in viewerUuids.toPlayers()) {
                getSentToPlayer(viewer).forEach { id ->
                    val point = id2point[id] ?: return@forEach
                    despawn(viewer, id, point, true)
                }
            }
        }

        clear()
    }

    @Synchronized
    private fun put(id: Int, point: VisualPoint) {
        id2point[id] = point
        point2Id[point] = id
    }

    @Synchronized
    private fun remove(location: Location): Pair<Int, VisualPoint>? {
        val point = point2Id.keys.find { it.location == location } ?: return null
        val id = point2Id.removeInt(point)

        if (id != Int.MIN_VALUE) {
            id2point.remove(id)
            return id to point
        }

        return null
    }

    @Synchronized
    private fun clear() {
        id2point.clear()
        point2Id.clear()
        sentToPlayers.clear()
    }

    @Synchronized
    private fun getSentToPlayer(player: Player) =
        sentToPlayers.computeIfAbsent(player) { mutableIntSetOf() }

    @Synchronized
    private fun onPlayerQuit(player: Player) {
        sentToPlayers.remove(player)
    }

    private fun spawn(player: Player, id: Int, point: VisualPoint) {
        if (player.seesLocation(point.location)) {
            spawnPacket(id, point).execute(player)
            getSentToPlayer(player).add(id)
        }
    }

    private fun despawn(player: Player, id: Int, point: VisualPoint, force: Boolean = false) {
        if (force || !player.seesLocation(point.location)) {
            nmsSpawnPackets.despawn(id).execute(player)
            getSentToPlayer(player).remove(id)
        }
    }

    private fun spawnPacket(id: Int, point: VisualPoint) =
        nmsSpawnPackets.spawnBlockDisplay(id, point.location, point.settings)

    private fun updatePositionPacket(id: Int, point: VisualPoint) =
        nmsSpawnPackets.teleport(id, point.location, point.location.yaw, point.location.pitch)

    override fun onViewerAdded(player: Player) {
        val sent = getSentToPlayer(player)
        val spawnOperation = PacketOperation.start()

        for (id in id2point.keys.toIntArray()) {
            val point = id2point[id] ?: continue
            if (player.seesLocation(point.location)) {
                spawnOperation + spawnPacket(id, point)
                sent.add(id)
            }
        }

        if (!(spawnOperation.isEmpty())) {
            spawnOperation.execute(player)
        }
    }

    override fun onViewerRemoved(player: Player) {
        val sent = getSentToPlayer(player)
        val despawnOperation = PacketOperation.start()

        for (id in sent.intIterator()) {
            val point = id2point[id] ?: continue
            if (!player.seesLocation(point.location)) {
                despawnOperation + nmsSpawnPackets.despawn(id)
            }
        }

        if (!(despawnOperation.isEmpty())) {
            despawnOperation.execute(player)
            sent.clear()
        }
    }

    override fun toString(): String {
        return "SurfVisualizerMultipleLocationsImpl(id2point=$id2point, sentToPlayers=$sentToPlayers, listener=$listener)"
    }

    private inner class VisualizerListener : Listener {
        @EventHandler
        fun onPlayerChunkLoad(event: PlayerChunkLoadEvent) {
            val player = event.player
            if (player.uniqueId !in viewerUuids) return

            val sent = getSentToPlayer(player)
            val toSent = mutableIntSetOf()
            val spawnOperation = PacketOperation.start()

            for (id in sent.intIterator()) {
                val point = id2point[id] ?: continue
                if (!player.seesLocation(point.location)) continue

                spawnOperation + spawnPacket(id, point)
                toSent.add(id)
            }

            if (!(spawnOperation.isEmpty())) {
                spawnOperation.execute(player)
                sent.addAll(toSent)
            }
        }

        @EventHandler
        fun onPlayerChunkUnload(event: PlayerChunkUnloadEvent) {
            val player = event.player
            if (player.uniqueId !in viewerUuids) return

            val sent = getSentToPlayer(player)
            val toDespawn = mutableIntSetOf()
            val despawnOperation = PacketOperation.start()

            for (id in sent.intIterator()) {
                val point = id2point[id] ?: continue
                if (!player.seesLocation(point.location)) {
                    toDespawn.add(id)
                    despawnOperation + nmsSpawnPackets.despawn(id)
                }
            }

            if (!(despawnOperation.isEmpty())) {
                despawnOperation.execute(player)
                sent.removeAll(toDespawn)
            }
        }

        @EventHandler
        fun onPlayerQuit(event: PlayerQuitEvent) {
            onPlayerQuit(event.player)
        }

        override fun toString(): String {
            return "VisualizerListener()"
        }
    }
}

data class VisualPoint(
    val location: Location,
    val settings: BlockDisplaySettings,
)