package dev.slne.surf.surfapi.bukkit.api.gui.context.abstract

import dev.slne.surf.surfapi.bukkit.api.gui.view.AbstractGuiView
import dev.slne.surf.surfapi.bukkit.api.gui.view.GuiView
import dev.slne.surf.surfapi.shared.api.util.InternalSurfApi
import org.bukkit.entity.Player

@InternalSurfApi
object NavigationHelper {
    fun navigateTo(currentView: GuiView, targetView: GuiView, player: Player, passProps: Boolean) {
        if (targetView is AbstractGuiView) {
            targetView.withParent(currentView)
            player.closeInventory()
            targetView.open(player)
        }
    }

    fun navigateBack(currentView: GuiView, player: Player) {
        val parent = currentView.parent

        if (parent is AbstractGuiView) {
            player.closeInventory()

            val resumeContext = parent.createResumeContext(player, currentView)

            parent.onResume(resumeContext)
            parent.open(player)
        } else {
            player.closeInventory()
        }
    }

    fun close(player: Player) {
        player.closeInventory()
    }

    fun update(view: GuiView, player: Player) {
        if (view is AbstractGuiView) {
            view.refreshInventory(player)
        }
    }
}