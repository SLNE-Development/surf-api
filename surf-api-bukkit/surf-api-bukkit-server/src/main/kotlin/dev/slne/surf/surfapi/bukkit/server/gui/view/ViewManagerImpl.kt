package dev.slne.surf.surfapi.bukkit.server.gui.view

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.bukkit.api.gui.view.GuiView
import dev.slne.surf.surfapi.bukkit.api.gui.view.ViewManager
import org.bukkit.entity.Player
import java.util.*

@AutoService(ViewManager::class)
class ViewManagerImpl : ViewManager {
    private val activePerPlayer = Caffeine.newBuilder()
        .weakValues()
        .build<UUID, GuiView>()

    override fun getActiveView(player: Player): GuiView? {
        return activePerPlayer.getIfPresent(player.uniqueId)
    }

    override fun setActiveView(
        player: Player,
        view: GuiView
    ) {
        activePerPlayer.put(player.uniqueId, view)
    }

    override fun removeActiveView(player: Player) {
        activePerPlayer.invalidate(player.uniqueId)
    }

    override fun toString(): String {
        return "ViewManagerImpl(activePerPlayer=$activePerPlayer)"
    }
}