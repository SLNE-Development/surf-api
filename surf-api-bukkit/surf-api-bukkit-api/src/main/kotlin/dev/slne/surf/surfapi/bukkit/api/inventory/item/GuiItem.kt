@file:OptIn(ExperimentalContracts::class)

package dev.slne.surf.surfapi.bukkit.api.inventory.item

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Creates a [GuiItem] from an existing [ItemStack] with an optional click [action].
 *
 * The [action] lambda is invoked on the [InventoryClickEvent] when the item is clicked.
 *
 * ```kotlin
 * val item = guiItem(ItemStack(Material.DIAMOND)) { isCancelled = true }
 * ```
 *
 * @param item the [ItemStack] to display
 * @param action the click handler invoked on [InventoryClickEvent]; defaults to a no-op
 * @return a new [GuiItem] wrapping [item] with the given [action]
 * @see guiItem
 */
fun guiItem(item: ItemStack, action: InventoryClickEvent.() -> Unit = {}): GuiItem {
    contract {
        callsInPlace(action, InvocationKind.UNKNOWN)
    }

    return GuiItem(item, action)
}

/**
 * Creates a [GuiItem] from a [Material] with an optional item customization block and click [action].
 *
 * A fresh [ItemStack] of [material] is created and passed to the [item] lambda for customization
 * (e.g. setting display name, lore, enchantments). The [action] lambda handles click events.
 *
 * ```kotlin
 * val item = guiItem(Material.DIAMOND, item = {
 *     itemMeta = itemMeta?.also { meta ->
 *         meta.displayName(Component.text("My Diamond"))
 *     }
 * }) { isCancelled = true }
 * ```
 *
 * @param material the [Material] of the [ItemStack]
 * @param item customization block applied to the new [ItemStack]
 * @param action the click handler; defaults to a no-op
 * @return a new [GuiItem] with the configured [ItemStack]
 * @see guiItem
 */
fun guiItem(
    material: Material,
    item: ItemStack.() -> Unit,
    action: InventoryClickEvent.() -> Unit = {}
): GuiItem {
    contract {
        callsInPlace(item, InvocationKind.EXACTLY_ONCE)
        callsInPlace(action, InvocationKind.UNKNOWN)
    }

    return GuiItem(ItemStack(material).apply(item), action)
}

/**
 * Creates a [GuiItem] from a [Material] with only an optional click [action].
 *
 * This is the simplest overload — no item customization is applied. Useful when you only
 * need a plain item with a click handler.
 *
 * ```kotlin
 * val barrier = guiItem(Material.BARRIER) { isCancelled = true }
 * ```
 *
 * @param material the [Material] of the [ItemStack]
 * @param action the click handler; defaults to a no-op
 * @return a new [GuiItem] with a plain [ItemStack] of [material]
 * @see guiItem
 */
fun guiItem(
    material: Material,
    action: InventoryClickEvent.() -> Unit = {}
) = guiItem(material, {}, action)