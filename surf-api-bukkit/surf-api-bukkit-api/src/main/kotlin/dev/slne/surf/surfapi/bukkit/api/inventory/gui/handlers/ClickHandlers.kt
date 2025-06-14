package dev.slne.surf.surfapi.bukkit.api.inventory.gui.handlers

import dev.slne.surf.surfapi.bukkit.api.inventory.dsl.GuiDsl
import dev.slne.surf.surfapi.core.api.util.InternalSurfApi
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

typealias ClickHandlerDsl = ClickHandlerScope.() -> Unit

@GuiDsl
interface ClickHandler : GuiHandler<InventoryClickEvent>

@JvmInline
@GuiDsl
value class ClickHandlerScope @InternalSurfApi constructor(val event: InventoryClickEvent) {
    val player
        get() = event.whoClicked as? Player ?: error("Click event is not triggered by a player")

    fun cancel() {
        event.isCancelled = true
    }
}

