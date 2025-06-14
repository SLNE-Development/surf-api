package dev.slne.surf.surfapi.bukkit.api.inventory.gui.handlers

import dev.slne.surf.surfapi.bukkit.api.inventory.dsl.GuiDsl
import dev.slne.surf.surfapi.core.api.util.InternalSurfApi
import org.bukkit.event.inventory.InventoryCloseEvent

typealias CloseHandlerDsl = CloseHandlerScope.() -> Unit

@GuiDsl
interface CloseHandler: GuiHandler<InventoryCloseEvent>

@JvmInline
@GuiDsl
value class CloseHandlerScope @InternalSurfApi constructor(val event: InventoryCloseEvent) {

}

