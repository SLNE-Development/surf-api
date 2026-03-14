package dev.slne.surf.surfapi.bukkit.api.inventory.framework.dsl

import me.devnatan.inventoryframework.context.IFOpenContext

/**
 * Cancels the open event associated with this [IFOpenContext].
 *
 * Sets [IFOpenContext.isCancelled] to `true`, preventing the inventory view from being shown
 * to the player.
 *
 * ```kotlin
 * onViewOpen { open ->
 *     if (!player.hasPermission("myPlugin.view")) open.cancel()
 * }
 * ```
 *
 * @receiver the [IFOpenContext] to cancel
 */
fun IFOpenContext.cancel() {
    isCancelled = true
}