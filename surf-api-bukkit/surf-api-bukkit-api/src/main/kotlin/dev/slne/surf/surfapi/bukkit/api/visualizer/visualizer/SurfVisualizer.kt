package dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.BlockDisplaySettings
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player

interface SurfVisualizer {
    fun addVisualLocation(
        visualLocation: Location,
        material: Material = DEFAULT_MATERIAL,
        consumer: BlockDisplaySettings.() -> Unit = {}
    ) = addVisualLocation(visualLocation, material, BlockDisplaySettings.create(consumer))

    fun addVisualLocation(
        visualLocation: Location,
        material: Material = DEFAULT_MATERIAL,
        consumer: BlockDisplaySettings
    )

    fun removeVisualLocation(visualLocation: Location)

    fun startVisualizing(): Boolean

    fun addViewer(player: Player)
    fun removeViewer(player: Player)

    fun stopVisualizing(): Boolean

    companion object {
        @JvmField
        val DEFAULT_MATERIAL: Material = Material.GLASS
    }
}
