package dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.function.Consumer

interface SurfVisualizer {
    fun addVisualLocation(visualLocation: Location)
    fun addVisualLocation(visualLocation: Location, material: Material)
    fun addVisualLocation(
        visualLocation: Location, material: Material,
        consumer: Consumer<Any> // TODO
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
