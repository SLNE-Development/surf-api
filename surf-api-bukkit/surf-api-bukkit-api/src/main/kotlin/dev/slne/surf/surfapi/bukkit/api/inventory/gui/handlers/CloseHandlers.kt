package dev.slne.surf.surfapi.bukkit.api.inventory.gui.handlers

import dev.slne.surf.surfapi.core.api.util.InternalSurfApi
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent

typealias CloseHandlerDsl = CloseHandlerScope.() -> Unit

interface CloseHandler: GuiHandler<InventoryCloseEvent>

@JvmInline
value class CloseHandlerScope @InternalSurfApi constructor(val event: InventoryCloseEvent) {
    val player
        get() = event.player as? Player ?: error("Close event is not triggered by a player")
}

