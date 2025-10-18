package dev.slne.surf.surfapi.bukkit.api.toast

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player

interface Toast {
    val icon: Material
    val text: Component
    val style: ToastStyle

    fun send(player: Player)
}
