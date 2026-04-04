@file:Suppress("UnstableApiUsage")

package dev.slne.surf.api.paper.inventory.framework.dsl

import dev.slne.surf.api.paper.inventory.framework.view.InventoryFrameworkDSL
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder
import me.devnatan.inventoryframework.context.RenderContext

/**
 * Configures the item component at the given linear slot index within this render context.
 *
 * Calls [RenderContext.slot] and immediately applies [block] to the returned
 * [BukkitItemComponentBuilder].
 *
 * ```kotlin
 * onFirstRender {
 *     slot(4) {
 *         withItem(Material.DIAMOND)
 *         onItemClick { cancel() }
 *     }
 * }
 * ```
 *
 * @receiver the [RenderContext] to configure
 * @param slot the linear slot index (0-based)
 * @param block configuration block applied to the [BukkitItemComponentBuilder]
 */
inline fun RenderContext.slot(
    slot: Int,
    block: @InventoryFrameworkDSL BukkitItemComponentBuilder.() -> Unit
) {
    slot(slot).apply(block)
}

/**
 * Configures the item component at the given row/column coordinates within this render context.
 *
 * ```kotlin
 * onFirstRender {
 *     slot(row = 2, column = 4) {
 *         withItem(Material.EMERALD)
 *     }
 * }
 * ```
 *
 * @receiver the [RenderContext] to configure
 * @param row the zero-based row index
 * @param column the zero-based column index
 * @param block configuration block applied to the [BukkitItemComponentBuilder]
 */
inline fun RenderContext.slot(
    row: Int,
    column: Int,
    block: @InventoryFrameworkDSL BukkitItemComponentBuilder.() -> Unit
) {
    slot(row, column).apply(block)
}

/**
 * Configures the item component at the first available slot in this render context.
 *
 * ```kotlin
 * onFirstRender {
 *     firstSlot { withItem(Material.APPLE) }
 * }
 * ```
 *
 * @receiver the [RenderContext] to configure
 * @param block configuration block applied to the [BukkitItemComponentBuilder]
 */
inline fun RenderContext.firstSlot(block: @InventoryFrameworkDSL BukkitItemComponentBuilder.() -> Unit) {
    firstSlot().apply(block)
}

/**
 * Configures the item component at the last available slot in this render context.
 *
 * ```kotlin
 * onFirstRender {
 *     lastSlot { withItem(Material.BARRIER) }
 * }
 * ```
 *
 * @receiver the [RenderContext] to configure
 * @param block configuration block applied to the [BukkitItemComponentBuilder]
 */
inline fun RenderContext.lastSlot(block: @InventoryFrameworkDSL BukkitItemComponentBuilder.() -> Unit) {
    lastSlot().apply(block)
}

/**
 * Configures the item component at the next available (empty) slot in this render context.
 *
 * ```kotlin
 * onFirstRender {
 *     for (item in items) {
 *         availableSlot { withItem(item.material) }
 *     }
 * }
 * ```
 *
 * @receiver the [RenderContext] to configure
 * @param block configuration block applied to the [BukkitItemComponentBuilder]
 */
inline fun RenderContext.availableSlot(block: @InventoryFrameworkDSL BukkitItemComponentBuilder.() -> Unit) {
    availableSlot().apply(block)
}

/**
 * Configures the item component mapped to a specific layout [character] in this render context.
 *
 * Layout characters are defined via [me.devnatan.inventoryframework.ViewConfigBuilder.layout].
 *
 * ```kotlin
 * // config:
 * layout {
 *     +"XXXXXXXXX"
 *     +"X       X"
 * }
 * // render:
 * onFirstRender {
 *     layoutSlot('X') { withItem(Material.STONE) }
 * }
 * ```
 *
 * @receiver the [RenderContext] to configure
 * @param character the layout character whose slots should be configured
 * @param block configuration block applied to the [BukkitItemComponentBuilder]
 */
inline fun RenderContext.layoutSlot(
    character: Char,
    block: @InventoryFrameworkDSL BukkitItemComponentBuilder.() -> Unit
) {
    layoutSlot(character).apply(block)
}

/**
 * Configures the item component at the result slot of this render context.
 *
 * The result slot is typically used in crafting-type views.
 *
 * ```kotlin
 * onFirstRender {
 *     resultSlot { withItem(Material.DIAMOND_SWORD) }
 * }
 * ```
 *
 * @receiver the [RenderContext] to configure
 * @param block configuration block applied to the [BukkitItemComponentBuilder]
 */
inline fun RenderContext.resultSlot(block: @InventoryFrameworkDSL BukkitItemComponentBuilder.() -> Unit) {
    resultSlot().apply(block)
}

