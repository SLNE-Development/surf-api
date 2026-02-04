package dev.slne.surf.surfapi.bukkit.api.gui.view

import dev.slne.surf.surfapi.core.api.util.requiredService
import org.bukkit.entity.Player

private val viewManager = requiredService<ViewManager>()

interface ViewManager {
    fun getActiveView(player: Player): GuiView?
    fun setActiveView(player: Player, view: GuiView)
    fun removeActiveView(player: Player)

    companion object : ViewManager by viewManager {
        val INSTANCE get() = viewManager
    }
}