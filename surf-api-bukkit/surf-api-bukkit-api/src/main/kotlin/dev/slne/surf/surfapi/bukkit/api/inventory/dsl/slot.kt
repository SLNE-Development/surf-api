package dev.slne.surf.surfapi.bukkit.api.inventory.dsl

import com.github.stefvanschie.inventoryframework.pane.util.Slot
import org.jetbrains.annotations.Range

fun slot(x: @Range(from = 0, to = 8) Int, y: @Range(from = 0, to = 5) Int) = Slot.fromXY(x, y)
fun slot(index: Int) = Slot.fromIndex(index)