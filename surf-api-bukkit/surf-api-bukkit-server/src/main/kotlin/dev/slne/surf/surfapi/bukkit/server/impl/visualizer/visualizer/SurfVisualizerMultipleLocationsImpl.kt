@file:OptIn(NmsUseWithCaution::class)

package dev.slne.surf.surfapi.bukkit.server.impl.visualizer.visualizer

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.nmsCommonBridge
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.BlockDisplaySettings
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.nmsSpawnPackets
import dev.slne.surf.surfapi.bukkit.api.region.TickThreadGuard
import dev.slne.surf.surfapi.bukkit.api.util.isChunkVisible
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizerMultipleLocations
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.UpdateStrategy
import dev.slne.surf.surfapi.bukkit.server.impl.visualizer.visualizerApiImpl
import dev.slne.surf.surfapi.core.api.util.*
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.entity.Player
import org.spongepowered.math.vector.Vector3d
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class SurfVisualizerMultipleLocationsImpl(world: World) : AbstractSurfVisualizerImpl(),
    SurfVisualizerMultipleLocations {
    private val worldReference = WeakReference(world)
    val world: World
        get() = worldReference.get() ?: error("World reference is no longer valid")

    fun getWorldIfPresent(): World? = worldReference.get()

    private val id2point = mutableInt2ObjectMapOf<VisualPoint>()
    private val point2Id =
        mutableObject2IntMapOf<VisualPoint>().apply { defaultReturnValue(Int.MIN_VALUE) }

    private val sentToPlayers = mutableObject2ObjectMapOf<UUID, IntSet>()

    private val lock = ReentrantReadWriteLock()

    override val visualLocations
        get() = readLocked { id2point.mapTo(mutableObjectSetOf(id2point.size)) { it.value.location } }.freeze()

    private inline fun <R> readLocked(block: () -> R): R = lock.read(block)
    private inline fun <R> writeLocked(block: () -> R): R = lock.write(block)

    init {
        cleaner.register(this, MultiLocationCleanupState(uid, id2point, internalViewerUuids, sentToPlayers, lock))
    }

    private class MultiLocationCleanupState(
        private val visualizerUuid: UUID,
        private val id2point: Int2ObjectMap<VisualPoint>,
        private val viewerUuids: MutableSet<UUID>,
        private val sentToPlayers: Object2ObjectMap<UUID, IntSet>,
        private val lock: ReentrantReadWriteLock,
    ) : CleanupState() {
        override fun cleanup() {
            if (id2point.isEmpty()) return

            val allIds = lock.read { IntOpenHashSet(id2point.keys) }
            val despawn = nmsSpawnPackets.despawn(allIds)

            for (uuid in viewerUuids) {
                visualizerApiImpl.onViewerRemoved(visualizerUuid, uuid)
                val player = Bukkit.getPlayer(uuid) ?: continue
                despawn.execute(player)
            }

            lock.write {
                id2point.clear()
                viewerUuids.clear()
                sentToPlayers.clear()
            }
        }
    }

    override fun onClose() {
        writeLocked {
            id2point.clear()
            point2Id.clear()
            sentToPlayers.clear()
        }
    }

    override fun startVisualizingInternal() {
        update()
    }

    override fun stopVisualizingInternal() {
        val sentByViewer = writeLocked {
            viewerUuids.associateWith { viewerUuid ->
                IntOpenHashSet(sentToPlayers.remove(viewerUuid) ?: IntOpenHashSet())
            }
        }

        for ((viewerUuid, ids) in sentByViewer) {
            if (ids.isEmpty()) continue

            val player = Bukkit.getPlayer(viewerUuid) ?: continue
            player.enterContextIfNeeded {
                nmsSpawnPackets.despawn(ids).execute(player)
            }
        }
    }

    override fun update(strategy: UpdateStrategy) {
        ensureNotClosed()
        if (!visualizing.get()) return
        if (!checkNotNullWorld()) return

        when (strategy) {
            UpdateStrategy.ALL -> {
                val pointsSnapshot = readLocked {
                    Int2ObjectOpenHashMap(id2point)
                }

                val version = currentStateVersion()
                for (viewer in viewerUuids) {
                    val player = Bukkit.getPlayer(viewer)
                    if (player == null) {
                        clearStaleData(viewer)
                        continue
                    }

                    player.enterContextIfNeeded {
                        if (!isActiveVersion(version)) return@enterContextIfNeeded

                        val previouslySent = drainSentToPlayer(viewer)
                        if (!previouslySent.isNullOrEmpty()) {
                            nmsSpawnPackets.despawn(previouslySent).execute(player)
                        }

                        if (!visualizing.get()) return@enterContextIfNeeded

                        val idsToMarkSent = mutableIntSetOf()
                        val spawn = PacketOperation.start()

                        pointsSnapshot.int2ObjectEntrySet().fastForEach { entry ->
                            val id = entry.intKey
                            val point = entry.value
                            if (player.isChunkVisible(world, point.chunkX, point.chunkZ) && idsToMarkSent.add(id)) {
                                spawn + spawnPacket(id, point)
                            }
                        }

                        writeLocked {
                            if (!visualizing.get()) return@enterContextIfNeeded
                            getOrCreateSentToPlayer(viewer).addAll(idsToMarkSent)
                        }

                        spawn.execute(player)
                    }
                }
            }

            UpdateStrategy.POSITION -> {
                val version = currentStateVersion()
                for (viewer in viewerUuids) {
                    val player = Bukkit.getPlayer(viewer)
                    if (player == null) {
                        clearStaleData(viewer)
                        continue
                    }

                    player.enterContextIfNeeded {
                        if (!isActiveVersion(version)) return@enterContextIfNeeded
                        if (!visualizing.get()) return@enterContextIfNeeded

                        val sentSnapshot = getSentToPlayerSnapshot(viewer)
                        val pointsSnapshot = readLocked { Int2ObjectOpenHashMap(id2point) }

                        val operation = PacketOperation.start()
                        val idsToRemove = mutableIntSetOf()

                        val iterator = sentSnapshot.iterator()
                        while (iterator.hasNext()) {
                            val id = iterator.nextInt()
                            val point = pointsSnapshot[id]

                            if (point == null) {
                                idsToRemove.add(id)
                                continue
                            }

                            if (player.isChunkVisible(world, point.chunkX, point.chunkZ)) {
                                if (!visualizing.get()) continue
                                operation + updatePositionPacket(id, point)
                            } else {
                                idsToRemove.add(id)
                            }
                        }

                        if (idsToRemove.isNotEmpty()) {
                            writeLocked {
                                getSentToPlayer(viewer)?.removeAll(idsToRemove)
                            }
                            nmsSpawnPackets.despawn(idsToRemove).execute(player)
                        }

                        operation.execute(player)
                    }
                }
            }
        }
    }

    override fun addVisualLocation(
        visualLocation: Vector3d,
        settings: BlockDisplaySettings,
    ) {
        ensureNotClosed()
        val id = nmsCommonBridge.nextEntityId()
        val point = VisualPoint(visualLocation, settings)
        put(id, point)

        if (!visualizing.get()) return
        if (!checkNotNullWorld()) return

        for (viewer in viewerUuids) {
            val player = Bukkit.getPlayer(viewer)
            if (player == null) {
                clearStaleData(viewer)
                continue
            }

            spawn(player, id, point)
        }
    }

    override fun addVisualLocations(locations: Collection<Pair<Vector3d, BlockDisplaySettings>>) {
        ensureNotClosed()
        if (locations.isEmpty()) return
        if (locations.size == 1) {
            addVisualLocation(locations.first().first, locations.first().second)
            return
        }

        if (!visualizing.get()) {
            for ((loc, setting) in locations) {
                addVisualLocation(loc, setting)
            }
            return
        }

        val toSpawn = mutableInt2ObjectMapOf<VisualPoint>()
        for ((loc, setting) in locations) {
            val id = nmsCommonBridge.nextEntityId()
            val point = VisualPoint(loc, setting)
            put(id, point)
            toSpawn[id] = point
        }

        val version = currentStateVersion()
        for (viewer in viewerUuids) {
            val player = Bukkit.getPlayer(viewer)
            if (player == null) {
                clearStaleData(viewer)
                continue
            }

            player.enterContextIfNeeded {
                if (!isActiveVersion(version)) return@enterContextIfNeeded
                if (!visualizing.get()) return@enterContextIfNeeded
                val idsToAdd = mutableIntSetOf()
                val spawnOperation = PacketOperation.start()

                toSpawn.int2ObjectEntrySet().fastForEach { entry ->
                    val id = entry.intKey
                    val point = entry.value

                    if (player.isChunkVisible(world, point.chunkX, point.chunkZ)) {
                        idsToAdd.add(id)
                        spawnOperation + spawnPacket(id, point)
                    }
                }

                writeLocked {
                    if (!visualizing.get()) return@enterContextIfNeeded
                    getOrCreateSentToPlayer(viewer).addAll(idsToAdd)
                }

                spawnOperation.execute(player)
            }
        }
    }

    override fun removeVisualLocation(visualLocation: Vector3d) {
        ensureNotClosed()
        val result = remove(visualLocation) ?: return
        val (id, point) = result

        if (!visualizing.get()) return
        if (!checkNotNullWorld()) return

        for (viewer in viewerUuids) {
            val player = Bukkit.getPlayer(viewer)
            if (player == null) {
                clearStaleData(viewer)
                continue
            }

            despawn(player, id, point, true)
        }
    }

    override fun clearVisualLocations() {
        ensureNotClosed()
        val idsToRemove = readLocked {
            IntOpenHashSet(id2point.keys)
        }

        val version = currentStateVersion()
        if (checkNotNullWorld() && idsToRemove.isNotEmpty()) {
            for (viewer in viewerUuids) {
                val player = Bukkit.getPlayer(viewer)
                if (player == null) {
                    clearStaleData(viewer)
                    continue
                }

                player.enterContextIfNeeded {
                    if (!isActiveVersion(version)) return@enterContextIfNeeded
                    nmsSpawnPackets.despawn(idsToRemove).execute(player)
                }
            }
        }

        clear()
    }

    private fun put(id: Int, point: VisualPoint) = writeLocked {
        id2point[id] = point
        point2Id[point] = id
    }

    private fun remove(location: Vector3d): Pair<Int, VisualPoint>? = writeLocked {
        val point = point2Id.keys.find { it.location == location } ?: return null
        val id = point2Id.removeInt(point)

        if (id != Int.MIN_VALUE) {
            id2point.remove(id)
            return id to point
        }

        return null
    }

    private fun clear() = writeLocked {
        id2point.clear()
        point2Id.clear()
        sentToPlayers.clear()
    }

    private fun getSentToPlayer(uuid: UUID): IntSet? = readLocked {
        sentToPlayers[uuid]
    }

    private fun getOrCreateSentToPlayer(uuid: UUID): IntSet = writeLocked {
        sentToPlayers.computeIfAbsent(uuid) { mutableIntSetOf() }
    }

    private fun getSentToPlayerSnapshot(uuid: UUID) = readLocked {
        sentToPlayers[uuid]?.let { IntOpenHashSet(it) } ?: IntOpenHashSet()
    }

    private fun drainSentToPlayer(uuid: UUID) = writeLocked {
        sentToPlayers.remove(uuid)
    }

    private fun spawn(player: Player, id: Int, point: VisualPoint) {
        val version = currentStateVersion()
        player.enterContextIfNeeded {
            if (!isActiveVersion(version)) return@enterContextIfNeeded
            if (visualizing.get() && player.isChunkVisible(world, point.chunkX, point.chunkZ)) {
                writeLocked {
                    getOrCreateSentToPlayer(player.uniqueId).add(id)
                }
                spawnPacket(id, point).execute(player)
            }
        }
    }

    private fun despawn(player: Player, id: Int, point: VisualPoint, force: Boolean = false) {
        val version = currentStateVersion()
        player.enterContextIfNeeded {
            if (!isActiveVersion(version)) return@enterContextIfNeeded
            if (force || !player.isChunkVisible(world, point.chunkX, point.chunkZ)) {
                writeLocked {
                    getOrCreateSentToPlayer(player.uniqueId).remove(id)
                }
                nmsSpawnPackets.despawn(id).execute(player)
            }
        }
    }

    private fun spawnPacket(id: Int, point: VisualPoint) =
        nmsSpawnPackets.spawnBlockDisplay(id, point.pos, point.settings)

    private fun updatePositionPacket(id: Int, point: VisualPoint) =
        nmsSpawnPackets.teleport(id, point.pos)

    override fun onPlayerReceiveChunk(player: Player, chunk: Chunk) {
        ensureNotClosed()
        if (!visualizing.get()) return
        TickThreadGuard.ensureTickThread(player, "Cannot receive async chunk load for visualizer")

        val entries = readLocked {
            Int2ObjectOpenHashMap(id2point)
        }

        val spawnOperation = PacketOperation.start()
        val idsToAdd = mutableIntSetOf()
        val sent = getSentToPlayerSnapshot(player.uniqueId)

        entries.int2ObjectEntrySet().fastForEach { entry ->
            val id = entry.intKey
            val point = entry.value
            if (world != chunk.world || point.chunkX != chunk.x || point.chunkZ != chunk.z) return@fastForEach
            if (sent.contains(id)) return@fastForEach

            spawnOperation + spawnPacket(id, point)
            idsToAdd.add(id)
        }

        if (idsToAdd.isNotEmpty()) {
            writeLocked {
                getOrCreateSentToPlayer(player.uniqueId).addAll(idsToAdd)
            }
        }

        spawnOperation.execute(player)
    }

    override fun onPlayerUnloadChunk(player: Player, chunk: Chunk) {
        ensureNotClosed()
        if (!visualizing.get()) return
        TickThreadGuard.ensureTickThread(player, "Cannot receive async chunk unload for visualizer")

        val sentSnapshot = getSentToPlayerSnapshot(player.uniqueId)
        val pointsSnapshot = readLocked { Int2ObjectOpenHashMap(id2point) }
        val despawn = mutableIntSetOf()

        val iterator = sentSnapshot.iterator()
        while (iterator.hasNext()) {
            val id = iterator.nextInt()
            val point = pointsSnapshot[id]
            if (point != null && (world != chunk.world || point.chunkX != chunk.x || point.chunkZ != chunk.z)) {
                continue
            }
            despawn.add(id)
        }

        if (despawn.isEmpty()) return
        writeLocked {
            getSentToPlayer(player.uniqueId)?.removeAll(despawn)
        }

        nmsSpawnPackets.despawn(despawn).execute(player)
    }

    override fun onViewerRemoved(player: Player) {
        ensureNotClosed()

        val sent = drainSentToPlayer(player.uniqueId) ?: return
        if (sent.isEmpty()) return

        player.enterContextIfNeeded {
            nmsSpawnPackets.despawn(sent).execute(player)
        }
    }

    override fun clearStaleData(uuid: UUID) {
        drainSentToPlayer(uuid)
    }

    fun checkNotNullWorld(): Boolean {
        if (worldReference.get() == null) {
            visualizing.set(false)
            writeLocked {
                sentToPlayers.clear()
            }
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