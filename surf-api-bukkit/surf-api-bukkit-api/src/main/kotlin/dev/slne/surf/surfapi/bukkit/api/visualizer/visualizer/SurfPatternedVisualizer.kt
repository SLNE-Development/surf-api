package dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer

import org.bukkit.Location
import org.bukkit.Material
import org.jetbrains.annotations.ApiStatus
import java.util.function.Consumer

interface SurfPatternedVisualizer : SurfVisualizer {
    @ApiStatus.Internal
    @ApiStatus.Obsolete
    override fun addVisualLocation(visualLocation: Location)

    @ApiStatus.Internal
    @ApiStatus.Obsolete
    override fun addVisualLocation(visualLocation: Location, material: Material)

    fun addVisualPoint(point: Location)

    fun removeVisualPoint(point: Location)

    fun setVisualMaterial(material: Material)

    fun setVisualMaterial(
        material: Material,
        consumer: Consumer<Any> // TODO
    )

    fun setVisualHeight(height: Int)

    fun setRenderAtHighestPoint(renderAtHighestPoint: Boolean)
}
