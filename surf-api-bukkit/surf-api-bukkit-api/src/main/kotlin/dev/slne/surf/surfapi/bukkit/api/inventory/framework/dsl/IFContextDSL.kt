package dev.slne.surf.surfapi.bukkit.api.inventory.framework.dsl

import me.devnatan.inventoryframework.RootView
import me.devnatan.inventoryframework.context.IFContext

fun IFContext.openForEveryone(other: RootView) = openForEveryone(other.javaClass)
fun IFContext.openForEveryone(other: RootView, initialData: Any) = openForEveryone(other.javaClass, initialData)