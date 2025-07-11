@file:OptIn(NmsUseWithCaution::class)

package dev.slne.surf.surfapi.bukkit.server.visualizer.visualizer

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.nmsCommonBridge
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.BlockDisplaySettings
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.nmsSpawnPackets
import dev.slne.surf.surfapi.bukkit.api.util.isChunkVisible
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizerMultipleLocations
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.UpdateStrategy
import dev.slne.surf.surfapi.bukkit.server.plugin
import dev.slne.surf.surfapi.core.api.util.*
import it.unimi.dsi.fastutil.ints.IntSet
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.entity.Player
import org.spongepowered.math.vector.Vector3d
import java.lang.ref.WeakReference
import java.util.*

class SurfVisualizerMultipleLocationsImpl(world: World) : AbstractSurfVisualizerImpl(),
    SurfVisualizerMultipleLocations {
    private val worldReference = WeakReference(world)
    val world: World
        get() = worldReference.get() ?: error("World reference is no longer valid")

    private val id2point = mutableInt2ObjectMapOf<VisualPoint>()
    private val point2Id =
        mutableObject2IntMapOf<VisualPoint>().apply { defaultReturnValue(Int.MIN_VALUE) }
    private val sentToPlayers = mutableObject2ObjectMapOf<UUID, IntSet>()

    override val visualLocations
        get() = id2point.mapTo(mutableObjectSetOf()) { it.value.location }.freeze()

    override fun startVisualizingInternal() {
        update()
    }

    override fun stopVisualizingInternal() {
        for (viewer in viewers) {
            val sentToPlayer = getSentToPlayer(viewer)
            nmsSpawnPackets.despawn(sentToPlayer).execute(viewer)
            sentToPlayer.clear()
        }
    }

    override fun update(strategy: UpdateStrategy) {
        if (!visualizing) return
        if (!checkNotNullWorld()) return

        when (strategy) {
            UpdateStrategy.ALL -> {
                for (viewer in viewers) {
                    plugin.launch(plugin.entityDispatcher(viewer)) {
                        val despawn = PacketOperation.start()
                        val sent = getSentToPlayer(viewer)

                        val sentIterator = sent.intIterator()
                        while (sentIterator.hasNext()) {
                            val id = sentIterator.nextInt()
                            val point = id2point[id] ?: continue
                            if (!viewer.isChunkVisible(world, point.chunkX, point.chunkZ)) {
                                despawn + nmsSpawnPackets.despawn(id)
                                sentIterator.remove()
                            }
                        }
                        despawn.execute(viewer)

                        val spawn = PacketOperation.start()
                        point2Id.object2IntEntrySet().fastForEach { entry ->
                            val point = entry.key
                            val id = entry.intValue

                            if (viewer.isChunkVisible(world, point.chunkX, point.chunkZ)) {
                                spawn + spawnPacket(id, point)
                                sent.add(id)
                            }
                        }
                        spawn.execute(viewer)
                    }
                }
            }

            UpdateStrategy.POSITION -> {
                for (viewer in viewers) {
                    plugin.launch(plugin.entityDispatcher(viewer)) {
                        val operation = PacketOperation.start()
                        val sent = getSentToPlayer(viewer)

                        val sentIterator = sent.intIterator()
                        while (sentIterator.hasNext()) {
                            val id = sentIterator.nextInt()
                            val point = id2point[id] ?: continue

                            if (viewer.isChunkVisible(world, point.chunkX, point.chunkZ)) {
                                operation + updatePositionPacket(id, point)
                            } else {
                                operation + nmsSpawnPackets.despawn(id)
                                sentIterator.remove()
                            }
                        }
                        operation.execute(viewer)
                    }
                }
            }
        }
    }

    override fun addVisualLocation(
        visualLocation: Vector3d,
        settings: BlockDisplaySettings,
    ) {
        val id = nmsCommonBridge.nextEntityId()
        val point = VisualPoint(visualLocation, settings)
        put(id, point)

        if (!visualizing) return
        if (!checkNotNullWorld()) return

        for (viewer in viewers) {
            spawn(viewer, id, point)
        }
    }

    override fun removeVisualLocation(visualLocation: Vector3d) {
        val result = remove(visualLocation) ?: return
        val (id, point) = result

        if (!visualizing) return
        if (!checkNotNullWorld()) return

        for (viewer in viewers) {
            despawn(viewer, id, point, true)
        }
    }

    override fun clearVisualLocations() {
        if (visualizing && checkNotNullWorld()) {
            for (viewer in viewers) {
                nmsSpawnPackets.despawn(getSentToPlayer(viewer)).execute(viewer)
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
    private fun remove(location: Vector3d): Pair<Int, VisualPoint>? {
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
        sentToPlayers.computeIfAbsent(player.uniqueId) { mutableIntSetOf() }

    private fun spawn(player: Player, id: Int, point: VisualPoint) {
        plugin.launch(plugin.entityDispatcher(player)) {
            if (player.isChunkVisible(world, point.chunkX, point.chunkZ)) {
                spawnPacket(id, point).execute(player)
                getSentToPlayer(player).add(id)
            }
        }
    }

    private fun despawn(player: Player, id: Int, point: VisualPoint, force: Boolean = false) {
        plugin.launch(plugin.entityDispatcher(player)) {
            if (force || !player.isChunkVisible(world, point.chunkX, point.chunkZ)) {
                nmsSpawnPackets.despawn(id).execute(player)
                getSentToPlayer(player).remove(id)
            }
        }
    }

    private fun spawnPacket(id: Int, point: VisualPoint) =
        nmsSpawnPackets.spawnBlockDisplay(id, point.pos, point.settings)

    private fun updatePositionPacket(id: Int, point: VisualPoint) =
        nmsSpawnPackets.teleport(id, point.pos)

    override fun onPlayerReceiveChunk(player: Player, chunk: Chunk) {
        val spawnOperation = PacketOperation.start()
        val sent = getSentToPlayer(player)

        point2Id.object2IntEntrySet().fastForEach { entry ->
            val point = entry.key
            val id = entry.intValue
            if (world != chunk.world || point.chunkX != chunk.x || point.chunkZ != chunk.z) return@fastForEach
            spawnOperation + spawnPacket(id, point)
            sent.add(id)
        }

        spawnOperation.execute(player)
    }

    override fun onPlayerUnloadChunk(player: Player, chunk: Chunk) {
        val despawnOperation = PacketOperation.start()
        val sent = getSentToPlayer(player)

        sent.intIterator().forEachRemaining { id ->
            val point = id2point[id] ?: return@forEachRemaining
            if (world != chunk.world || point.chunkX != chunk.x || point.chunkZ != chunk.z) return@forEachRemaining
            despawnOperation + nmsSpawnPackets.despawn(id)
            sent.remove(id)
        }

        despawnOperation.execute(player)
    }


    fun checkNotNullWorld(): Boolean {
        if (worldReference.get() == null) {
            visualizing = false
            viewerUuids.clear()
            log.atWarning()
                .log("World reference is no longer valid, stopping visualizer")
            return false
        }
        return true
    }

    override fun toString(): String {
        return "SurfVisualizerMultipleLocationsImpl(id2point=$id2point, sentToPlayers=$sentToPlayers)"
    }
}