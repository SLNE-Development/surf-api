package dev.slne.surf.surfapi.bukkit.api.inventory.dsl

import com.github.stefvanschie.inventoryframework.gui.type.util.MergedGui
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import com.github.stefvanschie.inventoryframework.pane.util.Slot

fun MergedGui.staticPane(
    slot: Slot,
    height: Int,
    length: Int = 9,
    init: (@PaneMarker StaticPane).() -> Unit
) {
    val pane = StaticPane(slot, length, height)
    pane.init()
    addPane(pane)
}