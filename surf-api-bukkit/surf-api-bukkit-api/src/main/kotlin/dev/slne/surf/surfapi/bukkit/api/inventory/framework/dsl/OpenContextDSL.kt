package dev.slne.surf.surfapi.bukkit.api.inventory.framework.dsl

import me.devnatan.inventoryframework.context.IFOpenContext

fun IFOpenContext.cancel() {
    isCancelled = true
}