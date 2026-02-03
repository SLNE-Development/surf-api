package dev.slne.surf.surfapi.bukkit.server.gui.context

import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.context.*
import dev.slne.surf.surfapi.bukkit.api.gui.view.GuiView
import dev.slne.surf.surfapi.bukkit.server.gui.view.BukkitGuiView
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

/**
 * Bukkit implementation of ViewContext.
 */
internal class BukkitViewContext(
    override val view: GuiView,
    override val player: Player
) : ViewContext {
    
    override fun navigateTo(view: GuiView, passProps: Boolean) {
        if (view is BukkitGuiView) {
            view.parent = this.view
            player.closeInventory()
            view.open(player)
        }
    }
    
    override fun navigateBack() {
        val parent = view.parent
        if (parent is BukkitGuiView) {
            player.closeInventory()
            val resumeContext = parent.createResumeContext(player, view)
            parent.onResume(resumeContext)
            parent.open(player)
        } else {
            close()
        }
    }
    
    override fun close() {
        player.closeInventory()
    }
    
    override fun update() {
        if (view is BukkitGuiView) {
            view.refreshInventory(player)
        }
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
        if (view is BukkitGuiView) {
            view.parent = this.view
            player.closeInventory()
            view.open(player)
        }
    }
    
    override fun navigateBack() {
        val parent = view.parent
        if (parent is BukkitGuiView) {
            player.closeInventory()
            val resumeContext = parent.createResumeContext(player, view)
            parent.onResume(resumeContext)
            parent.open(player)
        } else {
            close()
        }
    }
    
    override fun close() {
        player.closeInventory()
    }
    
    override fun update() {
        if (view is BukkitGuiView) {
            view.refreshInventory(player)
        }
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
    
    override fun renderComponent(slot: Int, component: Component) {
        bukkitView.addComponent(slot, component)
    }
    
    override fun clearSlot(slot: Int) {
        bukkitView.removeComponent(slot)
    }
    
    override fun setItem(slot: Int, item: ItemStack) {
        val inventory = player.openInventory.topInventory
        if (slot in 0 until inventory.size) {
            inventory.setItem(slot, item)
        }
    }
    
    override fun navigateTo(view: GuiView, passProps: Boolean) {
        if (view is BukkitGuiView) {
            view.parent = this.view
            player.closeInventory()
            view.open(player)
        }
    }
    
    override fun navigateBack() {
        val parent = view.parent
        if (parent is BukkitGuiView) {
            player.closeInventory()
            val resumeContext = parent.createResumeContext(player, view)
            parent.onResume(resumeContext)
            parent.open(player)
        } else {
            close()
        }
    }
    
    override fun close() {
        player.closeInventory()
    }
    
    override fun update() {
        bukkitView.refreshInventory(player)
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
        if (view is BukkitGuiView) {
            view.parent = this.view
            player.closeInventory()
            view.open(player)
        }
    }
    
    override fun navigateBack() {
        val parent = view.parent
        if (parent is BukkitGuiView) {
            player.closeInventory()
            val resumeContext = parent.createResumeContext(player, view)
            parent.onResume(resumeContext)
            parent.open(player)
        } else {
            close()
        }
    }
    
    override fun close() {
        player.closeInventory()
    }
    
    override fun update() {
        if (view is BukkitGuiView) {
            view.refreshInventory(player)
        }
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
        if (view is BukkitGuiView) {
            view.parent = this.view
            player.closeInventory()
            view.open(player)
        }
    }
    
    override fun navigateBack() {
        val parent = view.parent
        if (parent is BukkitGuiView) {
            player.closeInventory()
            val resumeContext = parent.createResumeContext(player, view)
            parent.onResume(resumeContext)
            parent.open(player)
        } else {
            close()
        }
    }
    
    override fun close() {
        player.closeInventory()
    }
    
    override fun update() {
        if (view is BukkitGuiView) {
            view.refreshInventory(player)
        }
    }
}
