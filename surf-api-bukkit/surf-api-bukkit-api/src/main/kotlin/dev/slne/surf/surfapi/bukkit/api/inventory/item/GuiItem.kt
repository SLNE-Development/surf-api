@file:OptIn(ExperimentalContracts::class)

package dev.slne.surf.surfapi.bukkit.api.inventory.item

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun guiItem(item: ItemStack, action: InventoryClickEvent.() -> Unit = {}): GuiItem {
    contract {
        callsInPlace(action, InvocationKind.UNKNOWN)
    }

    return GuiItem(item, action)
}

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

fun guiItem(
    material: Material,
    action: InventoryClickEvent.() -> Unit = {}
) = guiItem(material, {}, action)