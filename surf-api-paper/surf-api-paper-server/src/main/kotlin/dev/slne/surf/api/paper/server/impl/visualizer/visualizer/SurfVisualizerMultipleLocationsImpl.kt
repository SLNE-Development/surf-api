@file:OptIn(NmsUseWithCaution::class, ExperimentalVisualizerApi::class)

package dev.slne.surf.api.paper.server.impl.visualizer.visualizer

import dev.slne.surf.api.core.util.*
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsCommonBridge
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.nms.bridges.packets.entity.BlockDisplaySettings
import dev.slne.surf.api.paper.nms.bridges.packets.entity.SurfPaperNmsSpawnPackets
import dev.slne.surf.api.paper.region.TickThreadGuard
import dev.slne.surf.api.paper.server.impl.visualizer.SurfBukkitVisualizerApiImpl
import dev.slne.surf.api.paper.util.isChunkVisible
import dev.slne.surf.api.paper.visualizer.visualizer.ExperimentalVisualizerApi
import dev.slne.surf.api.paper.visualizer.visualizer.SurfVisualizerMultipleLocations
import dev.slne.surf.api.paper.visualizer.visualizer.UpdateStrategy
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
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

class SurfVisualizerMultipleLocationsImpl(
    world: World
) : AbstractSurfVisualizerImpl(), SurfVisualizerMultipleLocations {
    private val worldReference = WeakReference(world)
    val world: World
        get() = worldReference.get() ?: error("World reference is no longer valid")

    fun getWorldIfPresent(): World? = worldReference.get()

    private val id2point = mutableInt2ObjectMapOf<VisualPoint>()
    private val location2Ids = mutableObject2ObjectMapOf<Vector3d, IntSet>()
    private val chunk2Ids = mutableLong2ObjectMapOf<IntSet>()

    private val sentToPlayers = mutableObject2ObjectMapOf<UUID, IntSet>()

    private val lock = ReentrantReadWriteLock()

    override val visualLocations
        get() = readLocked {
            mutableObjectSetOf<Vector3d>(location2Ids.size).apply {
                addAll(location2Ids.keys)
            }
        }.freeze()

    private inline fun <R> readLocked(block: () -> R): R = lock.read(block)
    private inline fun <R> writeLocked(block: () -> R): R = lock.write(block)

    init {
        cleaner.register(
            this,
            MultiLocationCleanupState(
                uid,
                id2point,
                location2Ids,
                chunk2Ids,
                internalViewerUuids,
                sentToPlayers,
                lock
            )
        )
    }

    private class MultiLocationCleanupState(
        private val visualizerUuid: UUID,
        private val id2point: Int2ObjectMap<VisualPoint>,
        private val location2Ids: Object2ObjectMap<Vector3d, IntSet>,
        private val chunk2Ids: Long2ObjectMap<IntSet>,
        private val viewerUuids: MutableSet<UUID>,
        private val sentToPlayers: Object2ObjectMap<UUID, IntSet>,
        private val lock: ReentrantReadWriteLock,
    ) : CleanupState() {
        override fun cleanup() {
            val allIds = lock.read { IntOpenHashSet(id2point.keys) }
            val despawn = if (allIds.isEmpty()) null else SurfPaperNmsSpawnPackets.despawn(allIds)

            for (uuid in viewerUuids) {
                SurfBukkitVisualizerApiImpl.INSTANCE.onViewerRemoved(visualizerUuid, uuid)
                val player = Bukkit.getPlayer(uuid) ?: continue
                despawn?.execute(player)
            }

            lock.write {
                id2point.clear()
                location2Ids.clear()
                chunk2Ids.clear()
                viewerUuids.clear()
                sentToPlayers.clear()
            }
        }
    }

    override fun onClose() {
        writeLocked {
            id2point.clear()
            location2Ids.clear()
            chunk2Ids.clear()
            sentToPlayers.clear()
        }
    }

    override fun startVisualizingInternal() {
        update()
    }

    override fun stopVisualizingInternal() {
        val sentByViewer = writeLocked {
            mutableObject2ObjectMapOf<UUID, IntSet>(sentToPlayers.size).also { drained ->
                drained.putAll(sentToPlayers)
                sentToPlayers.clear()
            }
        }

        for ((viewerUuid, ids) in sentByViewer) {
            if (ids.isEmpty()) continue

            val player = Bukkit.getPlayer(viewerUuid) ?: continue
            player.enterContextIfNeeded {
                SurfPaperNmsSpawnPackets.despawn(ids).execute(player)
            }
        }
    }

    override fun update(strategy: UpdateStrategy) {
        ensureNotClosed()
        if (!visualizing.get()) return
        val currentWorld = getWorldIfPresent() ?: run {
            checkNotNullWorld()
            return
        }

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
                            SurfPaperNmsSpawnPackets.despawn(previouslySent).execute(player)
                        }

                        if (!visualizing.get()) return@enterContextIfNeeded

                        val spawn = PacketOperation.start()
                        writeLocked {
                            if (visualizing.get()) {
                                val sent = getOrCreateSentToPlayerLocked(viewer)
                                pointsSnapshot.int2ObjectEntrySet().fastForEach { entry ->
                                    val id = entry.intKey
                                    val point = entry.value
                                    if (id2point[id] === point &&
                                        player.isChunkVisible(
                                            currentWorld,
                                            point.chunkX,
                                            point.chunkZ
                                        ) && sent.add(id)
                                    ) {
                                        spawn + spawnPacket(id, point)
                                    }
                                }
                                if (sent.isEmpty()) {
                                    sentToPlayers.remove(viewer)
                                }
                            }
                        }

                        spawn.execute(player)
                    }
                }
            }

            UpdateStrategy.POSITION -> {
                val pointsSnapshot = readLocked { Int2ObjectOpenHashMap(id2point) }
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
                            ?: return@enterContextIfNeeded

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

                            if (player.isChunkVisible(currentWorld, point.chunkX, point.chunkZ)) {
                                if (!visualizing.get()) continue
                                operation + updatePositionPacket(id, point)
                            } else {
                                idsToRemove.add(id)
                            }
                        }

                        if (idsToRemove.isNotEmpty()) {
                            writeLocked {
                                sentToPlayers[viewer]?.removeAll(idsToRemove)
                            }
                            SurfPaperNmsSpawnPackets.despawn(idsToRemove).execute(player)
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
        val id = SurfPaperNmsCommonBridge.nextEntityId()
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
            val (location, settings) = locations.first()
            addVisualLocation(location, settings)
            return
        }

        val points = mutableInt2ObjectMapOf<VisualPoint>(locations.size)
        for ((loc, setting) in locations) {
            val id = SurfPaperNmsCommonBridge.nextEntityId()
            points[id] = VisualPoint(loc, setting)
        }
        addPreparedVisualLocations(points)
    }

    override fun addVisualLocations(
        visualLocations: Collection<Vector3d>,
        settings: BlockDisplaySettings,
    ) {
        ensureNotClosed()
        if (visualLocations.isEmpty()) return
        if (visualLocations.size == 1) {
            addVisualLocation(visualLocations.first(), settings)
            return
        }

        val points = mutableInt2ObjectMapOf<VisualPoint>(visualLocations.size)
        for (location in visualLocations) {
            val id = SurfPaperNmsCommonBridge.nextEntityId()
            points[id] = VisualPoint(location, settings)
        }
        addPreparedVisualLocations(points)
    }

    private fun addPreparedVisualLocations(points: Int2ObjectMap<VisualPoint>) {
        putAll(points)
        if (!visualizing.get()) return
        val currentWorld = getWorldIfPresent() ?: run {
            checkNotNullWorld()
            return
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
                val visibleIds = mutableIntSetOf()
                val spawnOperation = PacketOperation.start()

                val iterator = Int2ObjectMaps.fastIterator(points)
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    val id = entry.intKey
                    val point = entry.value

                    if (player.isChunkVisible(currentWorld, point.chunkX, point.chunkZ)) {
                        visibleIds.add(id)
                    }
                }

                if (visibleIds.isNotEmpty()) {
                    writeLocked {
                        if (visualizing.get()) {
                            val sent = getOrCreateSentToPlayerLocked(viewer)
                            val iterator = visibleIds.iterator()
                            while (iterator.hasNext()) {
                                val id = iterator.nextInt()
                                val point = points[id] ?: continue
                                if (id2point[id] === point && sent.add(id)) {
                                    spawnOperation + spawnPacket(id, point)
                                }
                            }
                            if (sent.isEmpty()) {
                                sentToPlayers.remove(viewer)
                            }
                        }
                    }
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
        val sentByViewer = writeLocked {
            val drained = mutableObject2ObjectMapOf<UUID, IntSet>(sentToPlayers.size)
            drained.putAll(sentToPlayers)
            sentToPlayers.clear()
            id2point.clear()
            location2Ids.clear()
            chunk2Ids.clear()
            drained
        }

        if (sentByViewer.isNotEmpty() && checkNotNullWorld()) {
            for ((viewerUuid, sentIds) in sentByViewer) {
                if (sentIds.isEmpty()) continue
                val player = Bukkit.getPlayer(viewerUuid)
                if (player == null) {
                    continue
                }

                player.enterContextIfNeeded {
                    SurfPaperNmsSpawnPackets.despawn(sentIds).execute(player)
                }
            }
        }
    }

    private fun put(id: Int, point: VisualPoint) = writeLocked {
        putLocked(id, point)
    }

    private fun remove(location: Vector3d): Pair<Int, VisualPoint>? = writeLocked {
        val locationIds = location2Ids[location] ?: return null
        val iterator = locationIds.iterator()
        if (!iterator.hasNext()) {
            location2Ids.remove(location)
            return null
        }

        val id = iterator.nextInt()
        iterator.remove()
        if (locationIds.isEmpty()) {
            location2Ids.remove(location)
        }

        val point = id2point.remove(id) ?: return null
        chunk2Ids[point.chunkKey]?.let { chunkIds ->
            chunkIds.remove(id)
            if (chunkIds.isEmpty()) {
                chunk2Ids.remove(point.chunkKey)
            }
        }
        id to point
    }

    private fun putAll(points: Int2ObjectMap<VisualPoint>) = writeLocked {
        val iterator = Int2ObjectMaps.fastIterator(points)
        while (iterator.hasNext()) {
            val entry = iterator.next()
            putLocked(entry.intKey, entry.value)
        }
    }

    private fun putLocked(id: Int, point: VisualPoint) {
        id2point[id] = point
        location2Ids.computeIfAbsent(point.location) { mutableIntSetOf() }.add(id)
        chunk2Ids.computeIfAbsent(point.chunkKey) { mutableIntSetOf() }.add(id)
    }

    private fun getOrCreateSentToPlayerLocked(uuid: UUID): IntSet =
        sentToPlayers.computeIfAbsent(uuid) { mutableIntSetOf() }

    private fun getSentToPlayerSnapshot(uuid: UUID) = readLocked {
        sentToPlayers[uuid]?.takeUnless(IntSet::isEmpty)?.let(::IntOpenHashSet)
    }

    private fun drainSentToPlayer(uuid: UUID) = writeLocked {
        sentToPlayers.remove(uuid)
    }

    private fun spawn(player: Player, id: Int, point: VisualPoint) {
        val version = currentStateVersion()
        player.enterContextIfNeeded {
            if (!isActiveVersion(version)) return@enterContextIfNeeded
            val currentWorld = getWorldIfPresent() ?: return@enterContextIfNeeded
            if (player.isChunkVisible(currentWorld, point.chunkX, point.chunkZ)) {
                val shouldSpawn = writeLocked {
                    id2point[id] === point &&
                            getOrCreateSentToPlayerLocked(player.uniqueId).add(id)
                }
                if (shouldSpawn) {
                    spawnPacket(id, point).execute(player)
                }
            }
        }
    }

    private fun despawn(player: Player, id: Int, point: VisualPoint, force: Boolean = false) {
        val version = currentStateVersion()
        player.enterContextIfNeeded {
            if (!isActiveVersion(version)) return@enterContextIfNeeded
            val currentWorld = getWorldIfPresent()
            if (force || currentWorld == null ||
                !player.isChunkVisible(currentWorld, point.chunkX, point.chunkZ)
            ) {
                val wasSent = writeLocked {
                    val sent = sentToPlayers[player.uniqueId] ?: return@writeLocked false
                    val removed = sent.remove(id)
                    if (sent.isEmpty()) {
                        sentToPlayers.remove(player.uniqueId)
                    }
                    removed
                }
                if (wasSent) {
                    SurfPaperNmsSpawnPackets.despawn(id).execute(player)
                }
            }
        }
    }

    private fun spawnPacket(id: Int, point: VisualPoint) =
        SurfPaperNmsSpawnPackets.spawnBlockDisplay(id, point.pos, point.settings)

    private fun updatePositionPacket(id: Int, point: VisualPoint) =
        SurfPaperNmsSpawnPackets.teleport(id, point.pos)

    override fun onPlayerReceiveChunk(player: Player, chunk: Chunk) {
        ensureNotClosed()
        if (!visualizing.get()) return
        TickThreadGuard.ensureTickThread(player, "Cannot receive async chunk load for visualizer")
        if (getWorldIfPresent() != chunk.world) return

        val entries = writeLocked {
            if (!visualizing.get()) return@writeLocked null
            val chunkIds = chunk2Ids[chunk.chunkKey] ?: return@writeLocked null
            val sent = getOrCreateSentToPlayerLocked(player.uniqueId)
            Int2ObjectOpenHashMap<VisualPoint>(chunkIds.size).also { result ->
                val iterator = chunkIds.iterator()
                while (iterator.hasNext()) {
                    val id = iterator.nextInt()
                    if (!sent.add(id)) continue
                    val point = id2point[id]
                    if (point == null) {
                        sent.remove(id)
                    } else {
                        result[id] = point
                    }
                }
                if (sent.isEmpty()) {
                    sentToPlayers.remove(player.uniqueId)
                }
            }
        }
        if (entries.isNullOrEmpty()) return

        val spawnOperation = PacketOperation.start()
        entries.int2ObjectEntrySet().fastForEach { entry ->
            val id = entry.intKey
            val point = entry.value
            spawnOperation + spawnPacket(id, point)
        }

        spawnOperation.execute(player)
    }

    override fun onPlayerUnloadChunk(player: Player, chunk: Chunk) {
        ensureNotClosed()
        if (!visualizing.get()) return
        TickThreadGuard.ensureTickThread(player, "Cannot receive async chunk unload for visualizer")
        if (getWorldIfPresent() != chunk.world) return

        val despawn = readLocked {
            val sent = sentToPlayers[player.uniqueId] ?: return@readLocked null
            val chunkIds = chunk2Ids[chunk.chunkKey] ?: return@readLocked null
            mutableIntSetOf(chunkIds.size).also { result ->
                val iterator = chunkIds.iterator()
                while (iterator.hasNext()) {
                    val id = iterator.nextInt()
                    if (sent.contains(id)) {
                        result.add(id)
                    }
                }
            }
        } ?: return

        if (despawn.isEmpty()) return
        writeLocked {
            val sent = sentToPlayers[player.uniqueId] ?: return@writeLocked
            sent.removeAll(despawn)
            if (sent.isEmpty()) {
                sentToPlayers.remove(player.uniqueId)
            }
        }

        SurfPaperNmsSpawnPackets.despawn(despawn).execute(player)
    }

    override fun onViewerRemoved(player: Player) {
        ensureNotClosed()

        val sent = drainSentToPlayer(player.uniqueId) ?: return
        if (sent.isEmpty()) return

        player.enterContextIfNeeded {
            SurfPaperNmsSpawnPackets.despawn(sent).execute(player)
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
