package dev.slne.surf.surfapi.bukkit.api.inventory.framework.dsl

import me.devnatan.inventoryframework.context.IFCloseContext

fun IFCloseContext.cancel() {
    isCancelled = true
}