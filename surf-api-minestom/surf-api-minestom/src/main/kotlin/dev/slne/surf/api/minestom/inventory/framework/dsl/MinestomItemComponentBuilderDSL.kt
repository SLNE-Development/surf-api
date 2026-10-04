package dev.slne.surf.api.minestom.inventory.framework.dsl

import dev.slne.surf.api.minestom.builder.ItemDsl
import dev.slne.surf.api.minestom.builder.buildItem
import dev.slne.surf.api.minestom.inventory.framework.view.InventoryFrameworkDSL
import me.devnatan.inventoryframework.component.MinestomItemComponentBuilder
import me.devnatan.inventoryframework.context.SlotClickContext
import me.devnatan.inventoryframework.context.SlotContext
import me.devnatan.inventoryframework.context.SlotRenderContext
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

/**
 * Sets the item displayed by this component using a [Material] and an optional builder block.
 *
 * ```kotlin
 * slot(4, 2) {
 *     withItem(Material.EMERALD) {
 *         displayName { primary("Gem") }
 *     }
 * }
 * ```
 *
 * @receiver the [MinestomItemComponentBuilder] to configure
 * @param material the [Material] of the item
 * @param amount the stack size; defaults to `1`
 * @param init optional customization block applied to the [ItemStack]
 * @return this builder for chaining
 */
inline fun MinestomItemComponentBuilder.withItem(
    material: Material,
    amount: Int = 1,
    init: (@InventoryFrameworkDSL ItemDsl).() -> Unit = {}
): MinestomItemComponentBuilder = this.withItem(buildItem(material, amount, init))

/**
 * Sets a dynamic render provider using a [Material] and an optional builder block.
 *
 * The item is rebuilt on every render cycle.
 *
 * ```kotlin
 * slot(4, 2) {
 *     renderWith(Material.GOLD_INGOT) {
 *         amount(currentGold)
 *     }
 * }
 * ```
 *
 * @receiver the [MinestomItemComponentBuilder] to configure
 * @param material the [Material] of the rendered item
 * @param amount the stack size; defaults to `1`
 * @param init optional customization block applied to each newly built [ItemStack]
 * @return this builder for chaining
 */
inline fun MinestomItemComponentBuilder.renderWith(
    material: Material,
    amount: Int = 1,
    crossinline init: (@InventoryFrameworkDSL ItemDsl).() -> Unit = {}
): MinestomItemComponentBuilder = this.renderWith { buildItem(material, amount, init) }

/**
 * Registers a render callback that is invoked each time the item component is rendered.
 *
 * ```kotlin
 * slot(0) {
 *     onItemRender {
 *         updateItemWith(Material.DIAMOND) { amount(player.level) }
 *     }
 * }
 * ```
 *
 * @receiver the [MinestomItemComponentBuilder] to configure
 * @param action the callback invoked with a [SlotRenderContext]
 */
inline fun MinestomItemComponentBuilder.onItemRender(crossinline action: @InventoryFrameworkDSL SlotRenderContext.() -> Unit) {
    this.onRender { context -> action(context) }
}

/**
 * Registers a click callback that is invoked when a player clicks this item's slot.
 *
 * ```kotlin
 * slot(0) {
 *     withItem(Material.BARRIER)
 *     onItemClick { cancel() }
 * }
 * ```
 *
 * @receiver the [MinestomItemComponentBuilder] to configure
 * @param action the callback invoked with a [SlotClickContext]
 */
inline fun MinestomItemComponentBuilder.onItemClick(crossinline action: @InventoryFrameworkDSL SlotClickContext.() -> Unit) {
    this.onClick { context -> action(context) }
}

/**
 * Registers an update callback that is invoked when this item's slot is updated.
 *
 * ```kotlin
 * slot(0) {
 *     withItem(Material.CLOCK)
 *     onItemUpdate { updateItemWith(Material.CLOCK) { amount(remainingSeconds) } }
 * }
 * ```
 *
 * @receiver the [MinestomItemComponentBuilder] to configure
 * @param action the callback invoked with a [SlotContext]
 */
inline fun MinestomItemComponentBuilder.onItemUpdate(crossinline action: @InventoryFrameworkDSL SlotContext.() -> Unit) {
    this.onUpdate { context -> action(context) }
}
