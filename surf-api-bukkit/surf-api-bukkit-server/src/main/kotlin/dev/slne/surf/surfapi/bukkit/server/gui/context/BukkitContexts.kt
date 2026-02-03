package dev.slne.surf.surfapi.bukkit.server.gui.context

import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.context.*
import dev.slne.surf.surfapi.bukkit.api.gui.toItemStack
import dev.slne.surf.surfapi.bukkit.api.gui.view.GuiView
import dev.slne.surf.surfapi.bukkit.server.gui.view.BukkitGuiView
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

/**
 * Shared navigation logic for all context implementations.
 */
private object NavigationHelper {
    fun navigateTo(currentView: GuiView, targetView: GuiView, player: Player, passProps: Boolean) {
        if (targetView is BukkitGuiView) {
            targetView.parent = currentView
            player.closeInventory()
            targetView.open(player)
        }
    }
    
    fun navigateBack(currentView: GuiView, player: Player) {
        val parent = currentView.parent
        if (parent is BukkitGuiView) {
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
        if (view is BukkitGuiView) {
            view.refreshInventory(player)
        }
    }
}

/**
 * Bukkit implementation of ViewContext.
 */
internal class BukkitViewContext(
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
}

/**
 * Bukkit implementation of ClickContext.
 */
internal class BukkitClickContext(
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
}

/**
 * Bukkit implementation of RenderContext.
 */
internal class BukkitRenderContext(
    override val view: GuiView,
    override val player: Player,
    private val bukkitView: BukkitGuiView
) : RenderContext {
    
    override fun renderComponent(slot: Slot, component: Component) {
        bukkitView.addComponent(slot, component)
    }
    
    override fun clearSlot(slot: Slot) {
        bukkitView.removeComponent(slot)
    }
    
    override fun setItem(slot: Slot, item: GuiItem) {
        val inventory = player.openInventory.topInventory
        if (slot.index in 0 until inventory.size) {
            inventory.setItem(slot.index, item.toItemStack())
        }
    }
    
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
}

/**
 * Bukkit implementation of LifecycleContext.
 */
internal class BukkitLifecycleContext(
    override val view: GuiView,
    override val player: Player,
    override val eventType: LifecycleEventType
) : LifecycleContext {
    
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
}

/**
 * Bukkit implementation of ResumeContext.
 */
internal class BukkitResumeContext(
    override val view: GuiView,
    override val player: Player,
    override val origin: GuiView?
) : ResumeContext {
    
    override val eventType: LifecycleEventType = LifecycleEventType.RESUME
    
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
}
