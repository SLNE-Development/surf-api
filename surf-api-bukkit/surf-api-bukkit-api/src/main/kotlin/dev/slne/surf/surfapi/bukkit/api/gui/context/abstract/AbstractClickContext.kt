package dev.slne.surf.surfapi.bukkit.api.gui.context.abstract

import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.context.ClickContext
import dev.slne.surf.surfapi.bukkit.api.gui.view.GuiView
import dev.slne.surf.surfapi.core.api.util.InternalSurfApi
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

@InternalSurfApi
class AbstractClickContext(
    override val view: GuiView,
    override val player: Player,
    override val event: InventoryClickEvent,
    override val component: Component?
) : ClickContext {
    override fun navigateTo(view: GuiView, passProps: Boolean) {
        NavigationHelper.navigateTo(this.view, view, player, passProps)
    }

    override fun navigateBack() {
        NavigationHelper.navigateBack(view, player)
    }

    override fun close() {
        NavigationHelper.close(player)
    }

    override fun update() {
        NavigationHelper.update(view, player)
    }

    override fun toString(): String {
        return "AbstractClickContext(view=$view, player=$player, event=$event, component=$component)"
    }
}