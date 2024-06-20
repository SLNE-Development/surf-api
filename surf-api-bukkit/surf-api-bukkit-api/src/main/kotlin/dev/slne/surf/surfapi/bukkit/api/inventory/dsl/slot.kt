package dev.slne.surf.surfapi.bukkit.api.inventory.dsl

import com.github.stefvanschie.inventoryframework.pane.util.Slot

fun slot(x: Int, y: Int) = Slot.fromXY(x, y)
fun slot(index: Int) = Slot.fromIndex(index)