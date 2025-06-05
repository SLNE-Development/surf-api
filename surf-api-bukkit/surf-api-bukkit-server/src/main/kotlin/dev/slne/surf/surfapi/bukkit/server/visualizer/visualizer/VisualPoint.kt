package dev.slne.surf.surfapi.bukkit.server.visualizer.visualizer

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.BlockDisplaySettings
import org.bukkit.Location

data class VisualPoint(
    val location: Location,
    val settings: BlockDisplaySettings,
)