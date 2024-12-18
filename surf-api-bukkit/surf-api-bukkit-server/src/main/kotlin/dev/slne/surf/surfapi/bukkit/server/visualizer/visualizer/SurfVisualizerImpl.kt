package dev.slne.surf.surfapi.bukkit.server.visualizer.visualizer

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.nmsCommonBridge
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.BlockDisplaySettings
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.nmsSpawnPackets
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.nmsPacketBridges
import dev.slne.surf.surfapi.bukkit.api.util.distanceSqt
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizer
import dev.slne.surf.surfapi.bukkit.server.plugin
import dev.slne.surf.surfapi.core.api.util.*
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.objects.ObjectList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.*
import kotlin.time.Duration.Companion.seconds

@OptIn(NmsUseWithCaution::class)
open class SurfVisualizerImpl : SurfVisualizer {
    protected val visualLocations = mutableObject2ObjectMapOf<Location, Material>().synchronize()
    private val viewers = mutableObjectSetOf<UUID>().synchronize()
    private val entities =
        mutableObject2ObjectMapOf<Location, Pair<Int, BlockDisplaySettings>>().synchronize()
    private val inDistance = mutableObject2ObjectMapOf<UUID, ObjectList<Location>>().synchronize()
    private val oldInDistance =
        mutableObject2ObjectMapOf<UUID, ObjectList<Location>>().synchronize()
    protected var job: Job? = null

    @Volatile
    protected var running: Boolean = false

    override fun addVisualLocation(
        visualLocation: Location,
        material: Material,
        settings: BlockDisplaySettings
    ) {
        visualLocations.put(visualLocation, material)
        entities[visualLocation] =
            nmsCommonBridge.nextEntityId() to settings
    }

    override fun removeVisualLocation(visualLocation: Location) {
        visualLocations.remove(visualLocation)
        val removed = entities.remove(visualLocation)
        if (removed != null) {
            val operation = nmsSpawnPackets.despawn(removed.first)
            forEachPlayerViewer { operation.execute(it) }
        }
    }

    protected fun removeAllVisualLocations() {
        visualLocations.clear()
        val ids = entities.mapTo(IntArrayList(entities.size)) { it.value.first }
        entities.clear()

        val operation = nmsSpawnPackets.despawn(ids)
        forEachPlayerViewer { operation.execute(it) }
    }

    override fun startVisualizing(): Boolean {
        if (running) {
            ComponentLogger.logger().warn("Tried to start visualizing while already running!")
            return false
        }

        if (visualLocations.isEmpty()) {
            ComponentLogger.logger().warn("Tried to start visualizing with no visual locations!")
            return false
        }

        val operation = createBatchSpawnOperation()
        forEachPlayerViewer { operation.execute(it) }

        job = launchVisualizerTask()
        return true
    }

    override fun addViewer(player: Player) {
        viewers.add(player.uniqueId)
        createBatchSpawnOperation().execute(player)
    }

    override fun removeViewer(player: Player) {
        viewers.remove(player.uniqueId)
        createBatchDespawnOperation().execute(player)
    }

    override fun stopVisualizing(): Boolean {
        if (!running) {
            ComponentLogger.logger().warn("Tried to stop visualizing while not running!")
            return false
        }

        val operation = createBatchDespawnOperation()
        forEachPlayerViewer { operation.execute(it) }
        entities.clear()

        job!!.cancel()
        return true
    }

    private fun createBatchSpawnOperation(): PacketOperation {
        val operation = nmsPacketBridges.createEmptyPacketOperation()
        entities.forEach { (location, idSettings) ->
            val (id, settings) = idSettings
            operation.add(nmsSpawnPackets.spawnBlockDisplay(id, location, settings))
        }
        return operation
    }

    private fun createBatchDespawnOperation(): PacketOperation {
        val ids = entities.mapTo(IntArrayList(entities.size)) { it.value.first }
        return nmsSpawnPackets.despawn(ids)
    }

    private fun launchVisualizerTask() = plugin.launch {
        while (true) {
            forEachPlayerViewer { player ->
                val oldLocations = inDistance.getOrElse(player.uniqueId) { mutableObjectListOf() }
                val newLocations = visualLocations.keys.asSequence()
                    .filter { it.world == player.world }
                    .filter { it distanceSqt player.location <= player.simulationDistance }
                    .toMutableObjectList()

                oldInDistance.put(player.uniqueId, oldLocations)
                inDistance.put(player.uniqueId, newLocations)

                // get old locations that are not in new locations
                val toRemove = newLocations.filterNot { it in oldLocations }
                nmsSpawnPackets.despawn(toRemove.mapNotNullTo(IntArrayList(toRemove.size)) { entities[it]?.first })
                    .execute(player)
            }

            delay(1.seconds)
        }
    }

    protected fun forEachPlayerViewer(block: (player: Player) -> Unit) {
        viewers.forEach { viewer ->
            val player = Bukkit.getPlayer(viewer)
            if (player != null) {
                block(player)
            }
        }
    }
}
