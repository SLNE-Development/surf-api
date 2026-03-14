package dev.slne.surf.surfapi.bukkit.api.inventory.framework.dsl

import me.devnatan.inventoryframework.context.IFCloseContext

/**
 * Cancels the inventory-close event associated with this [IFCloseContext].
 *
 * Sets [IFCloseContext.isCancelled] to `true`, preventing the inventory from closing.
 *
 * ```kotlin
 * onViewClose { close ->
 *     if (!allowClose) close.cancel()
 * }
 * ```
 *
 * @receiver the [IFCloseContext] to cancel
 */
fun IFCloseContext.cancel() {
    isCancelled = true
}