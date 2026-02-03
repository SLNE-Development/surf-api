package dev.slne.surf.surfapi.bukkit.server.inventory.component

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.surfapi.bukkit.api.inventory.component.Component
import dev.slne.surf.surfapi.bukkit.api.inventory.component.GuiComponent
import dev.slne.surf.surfapi.bukkit.api.inventory.component.ItemComponent
import dev.slne.surf.surfapi.bukkit.server.inventory.manager.InventoryManager
import dev.slne.surf.surfapi.bukkit.server.plugin
import kotlinx.coroutines.CoroutineScope
import net.kyori.adventure.text.Component as AdventureComponent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

/**
 * Implementation of GuiComponent that is Folia-safe.
 */
class GuiComponentImpl(
    override val title: AdventureComponent,
    override val rows: Int,
    private val itemComponents: List<ItemComponent>
) : GuiComponent {
    override var parent: Component? = null
    private val _children = mutableListOf<Component>()
    override val children: List<Component> get() = _children

    private val inventories = mutableMapOf<Player, Inventory>()

    init {
        // Add item components as children
        itemComponents.forEach { addChild(it) }
    }

    override fun addChild(child: Component) {
        _children.add(child)
        if (child is ItemComponent) {
            child as ItemComponentImpl
            child.parentGui = this
        }
    }

    override fun removeChild(child: Component) {
        _children.remove(child)
    }

    override suspend fun update() {
        // Propagate updates to all children
        children.forEach { child ->
            child.update()
        }
    }

    override suspend fun onMount() {
        // Components are mounted when added
    }

    override suspend fun onUnmount() {
        // Clean up inventories
        inventories.clear()
    }

    override fun shouldRender(): Boolean = true

    override suspend fun render(player: Player) {
        val inventory = getInventory(player) ?: return
        
        // Clear and render all items
        inventory.clear()
        children.filterIsInstance<ItemComponent>().forEach { item ->
            item.render(player)
        }
    }

    override suspend fun open(player: Player) {
        // Use Folia-safe scheduling
        plugin.launch(player) {
            val inventory = getOrCreateInventory(player)
            // Register before opening so the listener can find it
            InventoryManager.registerGui(player, this@GuiComponentImpl)
            render(player)
            player.openInventory(inventory)
        }
    }

    override suspend fun close(player: Player) {
        plugin.launch(player) {
            player.closeInventory()
            inventories.remove(player)
        }
    }

    override fun getInventory(player: Player): Inventory? {
        return inventories[player]
    }

    private fun getOrCreateInventory(player: Player): Inventory {
        return inventories.getOrPut(player) {
            Bukkit.createInventory(null, rows * 9, title)
        }
    }

    override suspend fun updateFor(player: Player) {
        render(player)
    }
}
