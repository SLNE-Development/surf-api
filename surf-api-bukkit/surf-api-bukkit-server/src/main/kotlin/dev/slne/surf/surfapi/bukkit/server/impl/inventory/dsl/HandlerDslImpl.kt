package dev.slne.surf.surfapi.bukkit.server.impl.inventory.dsl

import dev.slne.surf.surfapi.bukkit.api.inventory.gui.handlers.*
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent

@JvmInline
value class ClickHandlerScopeImpl(private val handler: ClickHandlerDsl) : ClickHandler {
    override fun handle(event: InventoryClickEvent) {
        ClickHandlerScope(event).handler()
    }
}

@JvmInline
value class CloseHandlerScopeImpl(private val handler: CloseHandlerDsl) : CloseHandler {
    override fun handle(event: InventoryCloseEvent) {
        CloseHandlerScope(event).handler()
    }
}

@JvmInline
value class DragHandlerScopeImpl(private val handler: DragHandlerDsl) : DragHandler {
    override fun handle(event: InventoryDragEvent) {
        DragHandlerScope(event).handler()
    }
}