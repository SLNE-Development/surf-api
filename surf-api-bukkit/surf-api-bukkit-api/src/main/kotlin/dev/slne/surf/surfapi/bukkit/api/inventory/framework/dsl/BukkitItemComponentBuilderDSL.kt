package dev.slne.surf.surfapi.bukkit.api.inventory.framework.dsl

import dev.slne.surf.surfapi.bukkit.api.builder.ItemDsl
import dev.slne.surf.surfapi.bukkit.api.builder.buildItem
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.InventoryFramworkDSL
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder
import me.devnatan.inventoryframework.context.SlotClickContext
import me.devnatan.inventoryframework.context.SlotContext
import me.devnatan.inventoryframework.context.SlotRenderContext
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType

inline fun BukkitItemComponentBuilder.withItem(
    type: ItemType,
    amount: Int = 1,
    init: (@InventoryFramworkDSL @ItemDsl ItemStack).() -> Unit = {}
): BukkitItemComponentBuilder = this.withItem(buildItem(type, amount, init))

inline fun BukkitItemComponentBuilder.renderWith(
    type: ItemType,
    amount: Int = 1,
    crossinline init: (@InventoryFramworkDSL @ItemDsl ItemStack).() -> Unit = {}
): BukkitItemComponentBuilder = this.renderWith { buildItem(type, amount, init) }

inline fun BukkitItemComponentBuilder.withItem(
    material: Material,
    amount: Int = 1,
    init: (@InventoryFramworkDSL @ItemDsl ItemStack).() -> Unit = {}
): BukkitItemComponentBuilder = this.withItem(buildItem(material, amount, init))

inline fun BukkitItemComponentBuilder.renderWith(
    material: Material,
    amount: Int = 1,
    crossinline init: (@InventoryFramworkDSL @ItemDsl ItemStack).() -> Unit = {}
): BukkitItemComponentBuilder = this.renderWith { buildItem(material, amount, init) }

inline fun BukkitItemComponentBuilder.onItemRender(crossinline action: @InventoryFramworkDSL SlotRenderContext.() -> Unit) {
    this.onRender { context -> action(context) }
}

inline fun BukkitItemComponentBuilder.onItemClick(crossinline action: @InventoryFramworkDSL SlotClickContext.() -> Unit) {
    this.onClick { context -> action(context) }
}

inline fun BukkitItemComponentBuilder.onItemUpdate(crossinline action: @InventoryFramworkDSL SlotContext.() -> Unit) {
    this.onUpdate { context -> action(context) }
}