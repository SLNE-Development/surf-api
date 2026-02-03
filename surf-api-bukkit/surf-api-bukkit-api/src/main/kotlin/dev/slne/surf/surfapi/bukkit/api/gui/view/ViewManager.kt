package dev.slne.surf.surfapi.bukkit.api.gui.view

import dev.slne.surf.surfapi.core.api.util.requiredService
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import org.bukkit.inventory.Inventory
import org.jetbrains.annotations.Unmodifiable

private val viewManager = requiredService<ViewManager>()

interface ViewManager {
    val views: @Unmodifiable Object2ObjectMap<Inventory, GuiView>

    fun registerView(inventory: Inventory, view: GuiView)

    fun unregisterView(view: GuiView)
    fun unregisterView(inventory: Inventory)

    fun findByInventory(inventory: Inventory): GuiView? {
        return views[inventory]
    }

    companion object : ViewManager by viewManager {
        val INSTANCE get() = viewManager
    }
}