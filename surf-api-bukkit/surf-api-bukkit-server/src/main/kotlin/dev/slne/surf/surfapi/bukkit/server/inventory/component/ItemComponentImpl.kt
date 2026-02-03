package dev.slne.surf.surfapi.bukkit.server.inventory.component

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.surfapi.bukkit.api.inventory.component.Component
import dev.slne.surf.surfapi.bukkit.api.inventory.component.GuiComponent
import dev.slne.surf.surfapi.bukkit.api.inventory.component.ItemComponent
import dev.slne.surf.surfapi.bukkit.server.plugin
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

/**
 * Implementation of ItemComponent that is Folia-safe.
 */
class ItemComponentImpl(
    override val slot: Int,
    override val itemStack: ItemStack?,
    override val canTake: Boolean = false,
    private val clickHandler: (suspend (Player, ClickType) -> Unit)? = null
) : ItemComponent {
    override var parent: Component? = null
    private val _children = mutableListOf<Component>()
    override val children: List<Component> get() = _children

    internal var parentGui: GuiComponent? = null

    override fun addChild(child: Component) {
        _children.add(child)
    }

    override fun removeChild(child: Component) {
        _children.remove(child)
    }

    override suspend fun update() {
        // Propagate updates to parent GUI
        parentGui?.let { gui ->
            // Get all viewers of the GUI and update for each
            plugin.launch {
                // This is a simplified approach - in practice, you'd track viewers
                // For now, we'll just trigger a general update
            }
        }

        // Propagate to children
        children.forEach { child ->
            child.update()
        }
    }

    override suspend fun onMount() {
        // Item is mounted when added to a GUI
    }

    override suspend fun onUnmount() {
        // Item is unmounted when removed from a GUI
    }

    override fun shouldRender(): Boolean = true

    override suspend fun render(player: Player) {
        val gui = parentGui ?: return
        val inventory = gui.getInventory(player) ?: return

        plugin.launch(player) {
            if (itemStack != null && slot in 0 until inventory.size) {
                inventory.setItem(slot, itemStack)
            }
        }
    }

    override suspend fun onClick(player: Player, clickType: ClickType) {
        if (clickHandler != null) {
            plugin.launch(player) {
                clickHandler.invoke(player, clickType)
            }
        }
    }

    override suspend fun updateFor(player: Player) {
        render(player)
    }
}
