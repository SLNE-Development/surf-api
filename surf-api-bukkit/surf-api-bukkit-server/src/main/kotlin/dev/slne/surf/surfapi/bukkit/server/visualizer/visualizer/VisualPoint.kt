@file:Suppress("UnstableApiUsage")

package dev.slne.surf.surfapi.bukkit.server.visualizer.visualizer

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.BlockDisplaySettings
import io.papermc.paper.math.Position
import org.bukkit.util.NumberConversions
import org.spongepowered.math.vector.Vector3d

data class VisualPoint(
    val location: Vector3d,
    val settings: BlockDisplaySettings,
) {
    val pos get() = Position.fine(location.x(), location.y(), location.z())
    val chunkX get() = NumberConversions.floor(location.x()) shr 4
    val chunkZ get() = NumberConversions.floor(location.z()) shr 4
}