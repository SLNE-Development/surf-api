package dev.slne.surf.surfapi.bukkit.api.inventory.framework.dsl

import dev.slne.surf.surfapi.bukkit.api.builder.ItemDsl
import dev.slne.surf.surfapi.bukkit.api.builder.buildItem
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.InventoryFrameworkDSL
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder
import me.devnatan.inventoryframework.context.SlotClickContext
import me.devnatan.inventoryframework.context.SlotContext
import me.devnatan.inventoryframework.context.SlotRenderContext
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType

/**
 * Sets the item displayed by this component using an [ItemType] and an optional builder block.
 *
 * Builds a new [ItemStack] via `buildItem(type, amount, init)` and passes it
 * to [BukkitItemComponentBuilder.withItem].
 *
 * ```kotlin
 * slot(4, 2) {
 *     withItem(ItemType.DIAMOND, amount = 1) {
 *         itemMeta = itemMeta?.also { it.displayName(Component.text("Special")) }
 *     }
 * }
 * ```
 *
 * @receiver the [BukkitItemComponentBuilder] to configure
 * @param type the [ItemType] of the item to display
 * @param amount the stack size; defaults to `1`
 * @param init optional customization block applied to the [ItemStack]
 * @return this builder for chaining
 */
inline fun BukkitItemComponentBuilder.withItem(
    type: ItemType,
    amount: Int = 1,
    init: (@InventoryFrameworkDSL @ItemDsl ItemStack).() -> Unit = {}
): BukkitItemComponentBuilder = this.withItem(buildItem(type, amount, init))

/**
 * Sets a dynamic render provider using an [ItemType] and an optional builder block.
 *
 * The item is rebuilt on every render cycle by calling `buildItem(type, amount, init)`.
 *
 * ```kotlin
 * slot(4, 2) {
 *     renderWith(ItemType.DIAMOND) {
 *         itemMeta = itemMeta?.also { it.displayName(Component.text("Dynamic")) }
 *     }
 * }
 * ```
 *
 * @receiver the [BukkitItemComponentBuilder] to configure
 * @param type the [ItemType] of the rendered item
 * @param amount the stack size; defaults to `1`
 * @param init optional customization block applied to each newly built [ItemStack]
 * @return this builder for chaining
 */
inline fun BukkitItemComponentBuilder.renderWith(
    type: ItemType,
    amount: Int = 1,
    crossinline init: (@InventoryFrameworkDSL @ItemDsl ItemStack).() -> Unit = {}
): BukkitItemComponentBuilder = this.renderWith { buildItem(type, amount, init) }

/**
 * Sets the item displayed by this component using a [Material] and an optional builder block.
 *
 * ```kotlin
 * slot(4, 2) {
 *     withItem(Material.EMERALD) {
 *         itemMeta = itemMeta?.also { it.displayName(Component.text("Gem")) }
 *     }
 * }
 * ```
 *
 * @receiver the [BukkitItemComponentBuilder] to configure
 * @param material the [Material] of the item
 * @param amount the stack size; defaults to `1`
 * @param init optional customization block applied to the [ItemStack]
 * @return this builder for chaining
 */
inline fun BukkitItemComponentBuilder.withItem(
    material: Material,
    amount: Int = 1,
    init: (@InventoryFrameworkDSL @ItemDsl ItemStack).() -> Unit = {}
): BukkitItemComponentBuilder = this.withItem(buildItem(material, amount, init))

/**
 * Sets a dynamic render provider using a [Material] and an optional builder block.
 *
 * The item is rebuilt on every render cycle.
 *
 * ```kotlin
 * slot(4, 2) {
 *     renderWith(Material.GOLD_INGOT) {
 *         amount = currentGold
 *     }
 * }
 * ```
 *
 * @receiver the [BukkitItemComponentBuilder] to configure
 * @param material the [Material] of the rendered item
 * @param amount the stack size; defaults to `1`
 * @param init optional customization block applied to each newly built [ItemStack]
 * @return this builder for chaining
 */
inline fun BukkitItemComponentBuilder.renderWith(
    material: Material,
    amount: Int = 1,
    crossinline init: (@InventoryFrameworkDSL @ItemDsl ItemStack).() -> Unit = {}
): BukkitItemComponentBuilder = this.renderWith { buildItem(material, amount, init) }

/**
 * Registers a render callback that is invoked each time the item component is rendered.
 *
 * ```kotlin
 * slot(0) {
 *     onItemRender {
 *         updateItemWith(Material.DIAMOND) { amount = player.level }
 *     }
 * }
 * ```
 *
 * @receiver the [BukkitItemComponentBuilder] to configure
 * @param action the callback invoked with a [SlotRenderContext]
 */
inline fun BukkitItemComponentBuilder.onItemRender(crossinline action: @InventoryFrameworkDSL SlotRenderContext.() -> Unit) {
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
 * @receiver the [BukkitItemComponentBuilder] to configure
 * @param action the callback invoked with a [SlotClickContext]
 */
inline fun BukkitItemComponentBuilder.onItemClick(crossinline action: @InventoryFrameworkDSL SlotClickContext.() -> Unit) {
    this.onClick { context -> action(context) }
}

/**
 * Registers an update callback that is invoked when this item's slot is updated.
 *
 * ```kotlin
 * slot(0) {
 *     withItem(Material.CLOCK)
 *     onItemUpdate { updateItemWith(Material.CLOCK) { amount = remainingSeconds } }
 * }
 * ```
 *
 * @receiver the [BukkitItemComponentBuilder] to configure
 * @param action the callback invoked with a [SlotContext]
 */
inline fun BukkitItemComponentBuilder.onItemUpdate(crossinline action: @InventoryFrameworkDSL SlotContext.() -> Unit) {
    this.onUpdate { context -> action(context) }
}