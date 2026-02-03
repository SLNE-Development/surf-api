package dev.slne.surf.surfapi.bukkit.api.inventory.dsl

import dev.slne.surf.surfapi.bukkit.api.inventory.component.GuiComponent
import dev.slne.surf.surfapi.bukkit.api.inventory.component.ItemComponent
import net.kyori.adventure.text.Component as AdventureComponent
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

/**
 * DSL marker for GUI builder.
 */
@DslMarker
annotation class GuiDsl

/**
 * Builder for creating a GUI component using Kotlin DSL.
 */
@GuiDsl
class GuiBuilder {
    var title: AdventureComponent = AdventureComponent.text("GUI")
    var rows: Int = 3
    private val items = mutableListOf<ItemBuilder>()

    /**
     * Adds an item to the GUI at the specified slot.
     */
    fun item(slot: Int, itemStack: ItemStack, block: ItemBuilder.() -> Unit = {}) {
        val builder = ItemBuilder(slot, itemStack)
        builder.block()
        items.add(builder)
    }

    /**
     * Fills a range of slots with the same item.
     */
    fun fill(slots: IntRange, itemStack: ItemStack, block: ItemBuilder.() -> Unit = {}) {
        for (slot in slots) {
            item(slot, itemStack, block)
        }
    }

    /**
     * Fills the border of the GUI with the specified item.
     */
    fun border(itemStack: ItemStack, block: ItemBuilder.() -> Unit = {}) {
        val totalSlots = rows * 9
        val lastRow = (rows - 1) * 9

        // Top row
        for (i in 0..8) {
            item(i, itemStack, block)
        }

        // Bottom row
        for (i in lastRow until totalSlots) {
            item(i, itemStack, block)
        }

        // Left and right columns
        for (row in 1 until rows - 1) {
            val start = row * 9
            item(start, itemStack, block) // Left
            item(start + 8, itemStack, block) // Right
        }
    }

    internal fun build(factory: GuiComponentFactory): GuiComponent {
        return factory.createGui(title, rows, items.map { it.build(factory) })
    }
}

/**
 * Builder for creating an item component.
 */
@GuiDsl
class ItemBuilder(
    val slot: Int,
    val itemStack: ItemStack
) {
    var canTake: Boolean = false
    var clickHandler: (suspend (Player, ClickType) -> Unit)? = null

    /**
     * Sets the click handler for this item.
     */
    fun onClick(handler: suspend (Player, ClickType) -> Unit) {
        clickHandler = handler
    }

    internal fun build(factory: GuiComponentFactory): ItemComponent {
        return factory.createItem(slot, itemStack, canTake, clickHandler)
    }
}

/**
 * Factory interface for creating GUI components.
 * This should be implemented by the server module.
 */
interface GuiComponentFactory {
    fun createGui(title: AdventureComponent, rows: Int, items: List<ItemComponent>): GuiComponent
    fun createItem(
        slot: Int,
        itemStack: ItemStack,
        canTake: Boolean,
        clickHandler: (suspend (Player, ClickType) -> Unit)?
    ): ItemComponent
}

/**
 * Creates a GUI using the Kotlin DSL.
 */
fun gui(factory: GuiComponentFactory, block: GuiBuilder.() -> Unit): GuiComponent {
    val builder = GuiBuilder()
    builder.block()
    return builder.build(factory)
}
