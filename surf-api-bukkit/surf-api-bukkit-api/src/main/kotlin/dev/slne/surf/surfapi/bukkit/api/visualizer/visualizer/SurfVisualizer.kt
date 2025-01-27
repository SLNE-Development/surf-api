package dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer

import it.unimi.dsi.fastutil.objects.ObjectSet
import org.bukkit.Material
import org.bukkit.entity.Player
import org.jetbrains.annotations.Unmodifiable
import java.util.*

@ExperimentalVisualizerApi
interface SurfVisualizer {
    val uid: UUID

    fun startVisualizing(): Boolean
    fun stopVisualizing(): Boolean
    fun isVisualizing(): Boolean

    val viewers: @Unmodifiable ObjectSet<Player>
    fun addViewer(player: Player)
    fun removeViewer(player: Player)
    fun clearViewers()
    fun hasViewers(): Boolean
    fun visibleTo(player: Player): Boolean

    fun update(strategy: UpdateStrategy = UpdateStrategy.ALL)

    companion object {
        @JvmField
        val DEFAULT_MATERIAL: Material = Material.GLASS
    }
}
