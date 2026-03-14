package dev.slne.surf.surfapi.bukkit.api.inventory.framework.dsl

import me.devnatan.inventoryframework.context.IFSlotClickContext

/**
 * Cancels the inventory-click event associated with this [IFSlotClickContext].
 *
 * Sets [IFSlotClickContext.isCancelled] to `true`, preventing the item from being moved
 * or the default click action from executing.
 *
 * ```kotlin
 * onViewClick { click ->
 *     click.cancel()
 * }
 * ```
 *
 * @receiver the [IFSlotClickContext] to cancel
 */
fun IFSlotClickContext.cancel() {
    isCancelled = true
}