package dev.slne.surf.surfapi.bukkit.server.gui.view

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.bukkit.api.gui.view.GuiView
import dev.slne.surf.surfapi.bukkit.api.gui.view.ViewManager
import dev.slne.surf.surfapi.core.api.util.freeze
import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf
import org.bukkit.inventory.Inventory

@AutoService(ViewManager::class)
class ViewManagerImpl : ViewManager {
    private val _views = mutableObject2ObjectMapOf<Inventory, GuiView>()
    override val views get() = _views.freeze()

    override fun registerView(
        inventory: Inventory,
        view: GuiView
    ) {
        _views[inventory] = view
    }

    override fun unregisterView(view: GuiView) {
        val entry = _views.entries.firstOrNull { it.value == view } ?: return
        _views.remove(entry.key)
    }

    override fun unregisterView(inventory: Inventory) {
        _views.remove(inventory)
    }
}