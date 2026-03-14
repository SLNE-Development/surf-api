@file:Suppress("UnstableApiUsage")

package dev.slne.surf.surfapi.bukkit.api.inventory.framework.dsl

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.InventoryFramworkDSL
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder
import me.devnatan.inventoryframework.context.RenderContext

inline fun RenderContext.slot(slot: Int, block: @InventoryFramworkDSL BukkitItemComponentBuilder.() -> Unit) {
    slot(slot).apply(block)
}

inline fun RenderContext.slot(
    row: Int,
    column: Int,
    block: @InventoryFramworkDSL BukkitItemComponentBuilder.() -> Unit
) {
    slot(row, column).apply(block)
}

inline fun RenderContext.firstSlot(block: @InventoryFramworkDSL BukkitItemComponentBuilder.() -> Unit) {
    firstSlot().apply(block)
}

inline fun RenderContext.lastSlot(block: @InventoryFramworkDSL BukkitItemComponentBuilder.() -> Unit) {
    lastSlot().apply(block)
}

inline fun RenderContext.availableSlot(block: @InventoryFramworkDSL BukkitItemComponentBuilder.() -> Unit) {
    availableSlot().apply(block)
}

inline fun RenderContext.layoutSlot(
    character: Char,
    block: @InventoryFramworkDSL BukkitItemComponentBuilder.() -> Unit
) {
    layoutSlot(character).apply(block)
}

inline fun RenderContext.resultSlot(block: @InventoryFramworkDSL BukkitItemComponentBuilder.() -> Unit) {
    resultSlot().apply(block)
}





