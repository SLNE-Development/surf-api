package dev.slne.surf.surfapi.bukkit.api.inventory

import org.bukkit.entity.Player

interface SinglePlayerGui : SurfGui {
    val player: Player

    fun open() = gui.show(player)
}