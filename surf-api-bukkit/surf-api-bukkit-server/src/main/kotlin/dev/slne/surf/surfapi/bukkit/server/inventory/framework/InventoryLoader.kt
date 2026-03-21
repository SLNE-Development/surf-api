package dev.slne.surf.surfapi.bukkit.server.inventory.framework

import dev.slne.surf.surfapi.bukkit.server.plugin
import me.devnatan.inventoryframework.ViewFrame

object InventoryLoader {
    init {
        InventoryViewRemapper.remap()
        CloseContextRemapper.remap()
    }

    lateinit var viewFrame: ViewFrame

    fun load() {
        viewFrame = ViewFrame.create(plugin)
    }

    fun enable() {
        viewFrame.register()
    }

    fun disable() {
        viewFrame.unregister()
    }
}