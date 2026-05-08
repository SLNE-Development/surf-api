package dev.slne.surf.api.paper.server.inventory.framework

import dev.slne.surf.api.paper.server.plugin
import me.devnatan.inventoryframework.ViewFrame

object InventoryLoader {
    init {
        McVersionRemapper.remap()
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