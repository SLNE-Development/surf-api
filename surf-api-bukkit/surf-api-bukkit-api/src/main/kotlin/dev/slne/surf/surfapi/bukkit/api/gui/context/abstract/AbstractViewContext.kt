package dev.slne.surf.surfapi.bukkit.api.gui.context.abstract

import dev.slne.surf.surfapi.bukkit.api.gui.context.ViewContext
import dev.slne.surf.surfapi.bukkit.api.gui.view.GuiView
import dev.slne.surf.surfapi.shared.api.util.InternalSurfApi
import org.bukkit.entity.Player

@InternalSurfApi
class AbstractViewContext(
    override val view: GuiView,
    override val player: Player
) : ViewContext {
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
        return "AbstractViewContext(view=$view, player=$player)"
    }
}