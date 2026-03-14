package dev.slne.surf.surfapi.bukkit.api.inventory.framework.dsl

import me.devnatan.inventoryframework.RootView
import me.devnatan.inventoryframework.context.IFConfinedContext

fun IFConfinedContext.openForPlayer(view: RootView) = openForPlayer(view.javaClass)
fun IFConfinedContext.openForPlayer(view: RootView, initialData: Any) = openForPlayer(view.javaClass, initialData)
