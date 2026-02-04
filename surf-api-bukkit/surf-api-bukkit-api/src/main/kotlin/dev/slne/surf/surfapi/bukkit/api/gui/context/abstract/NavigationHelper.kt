package dev.slne.surf.surfapi.bukkit.api.gui.context.abstract

import dev.slne.surf.surfapi.bukkit.api.gui.view.GuiView
import dev.slne.surf.surfapi.shared.api.util.InternalSurfApi
import org.bukkit.entity.Player

@InternalSurfApi
object NavigationHelper {
    fun navigateTo(currentView: GuiView, targetView: GuiView, player: Player, passProps: Boolean) {
        targetView.withParent(currentView)
        player.closeInventory()
        targetView.open(player)
    }

    fun navigateBack(currentView: GuiView, player: Player) {
        val parent = currentView.parent

        if (parent is GuiView) {
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
        view.refreshInventory(player)
    }
}