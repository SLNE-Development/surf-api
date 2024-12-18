package dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.BlockDisplaySettings
import org.bukkit.Location
import org.bukkit.Material
import org.jetbrains.annotations.ApiStatus

interface SurfPatternedVisualizer : SurfVisualizer {

    @ApiStatus.Obsolete
    override fun addVisualLocation(
        visualLocation: Location,
        material: Material,
        consumer: BlockDisplaySettings
    )

    fun addVisualPoint(point: Location)

    fun removeVisualPoint(point: Location)

    fun setVisualMaterial(
        material: Material,
        consumer: BlockDisplaySettings.() -> Unit = {}
    )

    fun setVisualHeight(height: Int)

    fun setRenderAtHighestPoint(renderAtHighestPoint: Boolean)
}
