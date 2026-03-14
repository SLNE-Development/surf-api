package dev.slne.surf.surfapi.bukkit.api.inventory.framework.dsl

import me.devnatan.inventoryframework.context.IFSlotClickContext

fun IFSlotClickContext.cancel() {
    isCancelled = true
}