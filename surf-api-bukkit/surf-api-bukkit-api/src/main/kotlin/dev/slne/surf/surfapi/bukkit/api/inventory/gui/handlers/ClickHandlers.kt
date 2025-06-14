package dev.slne.surf.surfapi.bukkit.api.inventory.gui.handlers

import dev.slne.surf.surfapi.core.api.util.InternalSurfApi
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

typealias ClickHandlerDsl = ClickHandlerScope.() -> Unit

interface ClickHandler : GuiHandler<InventoryClickEvent>

@JvmInline
value class ClickHandlerScope @InternalSurfApi constructor(val event: InventoryClickEvent) {
    val player
        get() = event.whoClicked as? Player ?: error("Click event is not triggered by a player")

    fun cancel() {
        event.isCancelled = true
    }
}

