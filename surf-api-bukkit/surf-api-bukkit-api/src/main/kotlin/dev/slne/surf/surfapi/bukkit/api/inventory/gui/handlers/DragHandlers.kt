package dev.slne.surf.surfapi.bukkit.api.inventory.gui.handlers

import dev.slne.surf.surfapi.core.api.util.InternalSurfApi
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryDragEvent

typealias DragHandlerDsl = DragHandlerScope.() -> Unit

interface DragHandler : GuiHandler<InventoryDragEvent>

@JvmInline
value class DragHandlerScope @InternalSurfApi constructor(val event: InventoryDragEvent) {
    val player
        get() = event.whoClicked as? Player ?: error("Drag event is not triggered by a player")

    fun cancel() {
        event.isCancelled = true
    }
}
