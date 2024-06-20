package dev.slne.surf.surfapi.bukkit.api.inventory.dsl

import com.github.stefvanschie.inventoryframework.pane.StaticPane
import com.github.stefvanschie.inventoryframework.pane.util.Slot

@PaneMarker
class StaticPaneScope(slot: Slot, length: Int, height: Int) : StaticPane(slot, length, height)