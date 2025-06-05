package dev.slne.surf.surfapi.bukkit.server.visualizer.visualizer

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.BlockDisplaySettings
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizer
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizerArea
import dev.slne.surf.surfapi.bukkit.server.visualizer.visualizerApiImpl
import dev.slne.surf.surfapi.core.api.math.BlockVec
import dev.slne.surf.surfapi.core.api.math.VoxelLineTracer
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import dev.slne.surf.surfapi.core.api.util.toObjectSet
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import org.bukkit.Location

class SurfVisualizerAreaImpl(
    initialSettings: BlockDisplaySettings?,
    initialEdges: Collection<Location>,
    private val delegate: SurfVisualizerMultipleLocationsImpl = visualizerApiImpl.createMultiLocationVisualizer(),
) : SurfVisualizer by delegate, SurfVisualizerArea {

    private val corners = ObjectLinkedOpenHashSet<Location>(initialEdges.size).apply {
        addAll(initialEdges)
    }

    override val cornerLocations by lazy { corners.toObjectSet() }

    override var settings: BlockDisplaySettings = initialSettings ?: BlockDisplaySettings.create {
        blockData = SurfVisualizer.DEFAULT_MATERIAL.createBlockData()
    }
        set(value) {
            field = value
            recompute()
        }

    init {
        if (initialEdges.isNotEmpty()) {
            recompute()
        }
    }

    override fun settings(consumer: BlockDisplaySettings.() -> Unit) {
        settings.consumer()
        recompute()
    }

    override fun addCornerLocation(location: Location) {
        if (corners.add(location)) {
            recompute()
        }
    }

    override fun removeCornerLocation(location: Location) {
        if (corners.remove(location)) {
            recompute()
        }
    }

    override fun clearCornerLocations() {
        if (corners.isNotEmpty()) {
            corners.clear()
            recompute()
        }
    }

    override fun setCornerLocations(locations: Collection<Location>) {
        if (corners != locations) {
            corners.clear()
            corners.addAll(locations)
            recompute()
        }
    }

    private fun recompute() {
        delegate.clearVisualLocations()
        if (corners.size < 2) return
        val world = corners.first().world ?: error("Location does not have a world")
        val cornerBlocks = corners.mapTo(mutableObjectListOf(corners.size)) {
            BlockVec(
                it.blockX,
                it.blockY,
                it.blockZ
            )
        }

        val edgePoints = ObjectLinkedOpenHashSet<BlockVec>()
        for (i in cornerBlocks.indices) {
            edgePoints += VoxelLineTracer.trace(
                cornerBlocks[i],
                cornerBlocks[(i + 1) % cornerBlocks.size]
            )
        }

        edgePoints.forEach {
            delegate.addVisualLocation(
                Location(
                    world,
                    it.x.toDouble(),
                    it.y.toDouble(),
                    it.z.toDouble()
                ), settings
            )
        }
    }
}