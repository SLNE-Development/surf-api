package dev.slne.surf.surfapi.bukkit.server.inventory.component

import dev.slne.surf.surfapi.bukkit.api.inventory.component.ItemComponent
import dev.slne.surf.surfapi.bukkit.api.inventory.dsl.GuiComponentFactory
import dev.slne.surf.surfapi.bukkit.api.inventory.component.GuiComponent
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

/**
 * Factory implementation for creating GUI components.
 */
object GuiComponentFactoryImpl : GuiComponentFactory {
    override fun createGui(title: Component, rows: Int, items: List<ItemComponent>): GuiComponent {
        return GuiComponentImpl(title, rows, items)
    }

    override fun createItem(
        slot: Int,
        itemStack: ItemStack,
        canTake: Boolean,
        clickHandler: (suspend (Player, ClickType) -> Unit)?
    ): ItemComponent {
        return ItemComponentImpl(slot, itemStack, canTake, clickHandler)
    }
}
