@file:OptIn(NmsUseWithCaution::class, ExperimentalVisualizerApi::class)

package dev.slne.surf.api.paper.server.impl.visualizer.visualizer

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
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
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
    world: World,
) : AbstractSurfVisualizerImpl(), SurfVisualizerMultipleLocations {
    private val worldReference = WeakReference(world)
    val world: World
        get() = worldReference.get() ?: error("World reference is no longer valid")

    fun getWorldIfPresent(): World? = worldReference.get()

    private val id2point = Int2ObjectOpenHashMap<VisualPoint>()
    private val point2Id = Object2IntOpenHashMap<VisualPoint>().apply {
        defaultReturnValue(NO_ID)
    }

    private val sentToPlayers = HashMap<UUID, IntOpenHashSet>()

    private val lock = ReentrantReadWriteLock()

    override val visualLocations: Set<Vector3d>
        get() = readLocked { id2point.values.mapTo(mutableSetOf()) { it.location } }

    private inline fun <R> readLocked(block: () -> R): R = lock.read(block)
    private inline fun <R> writeLocked(block: () -> R): R = lock.write(block)

    init {
        cleaner.register(
            this,
            MultiLocationCleanupState(uid, id2point, internalViewerUuids, sentToPlayers, lock),
        )
    }

    private class MultiLocationCleanupState(
        private val visualizerUuid: UUID,
        private val id2point: Int2ObjectMap<VisualPoint>,
        private val viewerUuids: MutableSet<UUID>,
        private val sentToPlayers: MutableMap<UUID, IntOpenHashSet>,
        private val lock: ReentrantReadWriteLock,
    ) : CleanupState() {
        override fun cleanup() {
            if (id2point.isEmpty()) return

            val allIds = lock.read { id2point.keys.toIntArray() }
            val despawn = SurfPaperNmsSpawnPackets.instance.despawn(*allIds)

            for (uuid in viewerUuids) {
                SurfBukkitVisualizerApiImpl.instance.onViewerRemoved(visualizerUuid, uuid)
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
            viewerUuids.associateWith { viewerUuid -> sentToPlayers.remove(viewerUuid) }
        }

        for ((viewerUuid, ids) in sentByViewer) {
            if (ids.isNullOrEmpty()) continue

            val player = Bukkit.getPlayer(viewerUuid) ?: continue

            player.enterContextIfNeeded {
                despawnPacket(ids).execute(player)
            }
        }
    }

    override fun update(strategy: UpdateStrategy) {
        ensureNotClosed()
        if (!visualizing.get()) return
        if (!checkNotNullWorld()) return

        when (strategy) {
            UpdateStrategy.ALL -> {
                val pointsSnapshot = snapshotPoints()

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
                            despawnPacket(previouslySent).execute(player)
                        }

                        if (!visualizing.get()) return@enterContextIfNeeded

                        val idsToMarkSent = IntOpenHashSet()
                        val spawn = PacketOperation.start()

                        for (entry in Int2ObjectMaps.fastIterable(pointsSnapshot)) {
                            val id = entry.intKey
                            val point = entry.value

                            if (player.isChunkVisible(
                                    world,
                                    point.chunkX,
                                    point.chunkZ,
                                ) && idsToMarkSent.add(id)
                            ) {
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

                        val sentIds = copySentIds(viewer)
                        val pointsSnapshot = snapshotPoints()

                        val operation = PacketOperation.start()
                        val idsToRemove = IntOpenHashSet()

                        for (id in sentIds) {
                            val point = pointsSnapshot.get(id)

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
                            despawnPacket(idsToRemove).execute(player)
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
        val id = SurfPaperNmsCommonBridge.instance.nextEntityId()
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

        val toSpawn = Int2ObjectOpenHashMap<VisualPoint>()

        for ((loc, setting) in locations) {
            val id = SurfPaperNmsCommonBridge.instance.nextEntityId()
            val point = VisualPoint(loc, setting)
            put(id, point)
            toSpawn.put(id, point)
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

                val idsToAdd = IntOpenHashSet()
                val spawnOperation = PacketOperation.start()

                for (entry in Int2ObjectMaps.fastIterable(toSpawn)) {
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
            id2point.keys.toIntArray()
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

                    SurfPaperNmsSpawnPackets.instance.despawn(*idsToRemove).execute(player)
                }
            }
        }

        clear()
    }

    private fun put(id: Int, point: VisualPoint) {
        writeLocked {
            id2point.put(id, point)
            point2Id.put(point, id)
        }
    }

    private fun remove(location: Vector3d): Pair<Int, VisualPoint>? {
        writeLocked {
            val point = point2Id.keys.find { it.location == location } ?: return null
            val id = point2Id.removeInt(point)
            if (id == NO_ID) return null

            id2point.remove(id)

            return id to point
        }
    }

    private fun clear() {
        writeLocked {
            id2point.clear()
            point2Id.clear()
            sentToPlayers.clear()
        }
    }

    private fun snapshotPoints(): Int2ObjectOpenHashMap<VisualPoint> {
        return readLocked {
            Int2ObjectOpenHashMap(id2point)
        }
    }

    private fun getSentToPlayer(uuid: UUID): IntOpenHashSet? {
        return readLocked {
            sentToPlayers[uuid]
        }
    }

    private fun getOrCreateSentToPlayer(uuid: UUID): IntOpenHashSet {
        return writeLocked {
            sentToPlayers.computeIfAbsent(uuid) { IntOpenHashSet() }
        }
    }

    private fun getSentToPlayerSnapshot(uuid: UUID): IntOpenHashSet {
        return readLocked {
            val sent = sentToPlayers[uuid] ?: return@readLocked IntOpenHashSet()

            IntOpenHashSet(sent)
        }
    }

    private fun copySentIds(uuid: UUID): IntArray {
        return readLocked {
            sentToPlayers[uuid]?.toIntArray() ?: IntArray(0)
        }
    }

    private fun drainSentToPlayer(uuid: UUID): IntOpenHashSet? {
        return writeLocked {
            sentToPlayers.remove(uuid)
        }
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
                SurfPaperNmsSpawnPackets.instance.despawn(id).execute(player)
            }
        }
    }

    private fun despawnPacket(ids: IntOpenHashSet): PacketOperation {
        return SurfPaperNmsSpawnPackets.instance.despawn(*ids.toIntArray())
    }

    private fun spawnPacket(id: Int, point: VisualPoint): PacketOperation {
        return SurfPaperNmsSpawnPackets.instance.spawnBlockDisplay(id, point.pos, point.settings)
    }

    private fun updatePositionPacket(id: Int, point: VisualPoint): PacketOperation {
        return SurfPaperNmsSpawnPackets.instance.teleport(id, point.pos)
    }

    override fun onPlayerReceiveChunk(player: Player, chunk: Chunk) {
        ensureNotClosed()
        if (!visualizing.get()) return

        TickThreadGuard.instance.ensureTickThread(
            player,
            "Cannot receive async chunk load for visualizer",
        )

        val entries = snapshotPoints()

        val spawnOperation = PacketOperation.start()
        val idsToAdd = IntOpenHashSet()
        val sent = getSentToPlayerSnapshot(player.uniqueId)

        for (entry in Int2ObjectMaps.fastIterable(entries)) {
            val id = entry.intKey
            val point = entry.value

            if (world != chunk.world || point.chunkX != chunk.x || point.chunkZ != chunk.z) continue
            if (sent.contains(id)) continue

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

        TickThreadGuard.instance.ensureTickThread(
            player,
            "Cannot receive async chunk unload for visualizer",
        )

        val sentIds = copySentIds(player.uniqueId)
        val pointsSnapshot = snapshotPoints()
        val despawn = IntOpenHashSet()

        for (id in sentIds) {
            val point = pointsSnapshot.get(id)

            if (point != null &&
                (world != chunk.world || point.chunkX != chunk.x || point.chunkZ != chunk.z)
            ) {
                continue
            }

            despawn.add(id)
        }

        if (despawn.isEmpty()) return

        writeLocked {
            getSentToPlayer(player.uniqueId)?.removeAll(despawn)
        }

        despawnPacket(despawn).execute(player)
    }

    override fun onViewerRemoved(player: Player) {
        ensureNotClosed()

        val sent = drainSentToPlayer(player.uniqueId) ?: return
        if (sent.isEmpty()) return

        player.enterContextIfNeeded {
            despawnPacket(sent).execute(player)
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

    private companion object {
        const val NO_ID = -1
    }
}
