package dev.slne.surf.api.paper.inventory.framework.dsl

import me.devnatan.inventoryframework.context.IFCloseContext

/**
 * Cancels the inventory-close event associated with this [IFCloseContext].
 *
 * Sets [IFCloseContext.isCancelled] to `true`, preventing the inventory from closing.
 *
 * ```kotlin
 * onClose { close ->
 *     if (!allowClose) close.cancel()
 * }
 * ```
 *
 * @receiver the [IFCloseContext] to cancel
 */
fun IFCloseContext.cancel() {
    isCancelled = true
}