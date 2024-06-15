package dev.slne.surf.surfapi.bukkit.api.inventory.dsl

import com.github.stefvanschie.inventoryframework.gui.type.util.MergedGui
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import com.github.stefvanschie.inventoryframework.pane.util.Slot
import dev.slne.surf.surfapi.bukkit.api.inventory.types.SurfChestGui
import dev.slne.surf.surfapi.bukkit.api.inventory.types.SurfChestSinglePlayerGui
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.jetbrains.annotations.Range

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@DslMarker
annotation class MenuMarker

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

fun menu(
    title: Component,
    rows: @Range(from = 2, to = 6) Int = 6,
    init: @MenuMarker SurfChestGui.() -> Unit
): SurfChestGui {
    val menu = SurfChestGui(title, rows)
    menu.init()
    return menu
}

fun playerMenu(
    title: Component,
    player: Player,
    rows: @Range(from = 2, to = 6) Int = 6,
    init: @MenuMarker SurfChestSinglePlayerGui.() -> Unit
): SurfChestSinglePlayerGui {
    val menu = SurfChestSinglePlayerGui(title, player, rows)
    menu.init()
    return menu
}


fun SurfChestGui.childMenu(
    title: Component,
    rows: @Range(from = 2, to = 6) Int,
    init: @MenuMarker SurfChestGui.() -> Unit
): SurfChestGui {
    val menu = SurfChestGui(title, rows, this)
    menu.init()
    return menu
}

fun SurfChestSinglePlayerGui.childMenu(
    title: Component,
    rows: @Range(from = 2, to = 6) Int,
    init: @MenuMarker SurfChestSinglePlayerGui.() -> Unit
): SurfChestSinglePlayerGui {
    val menu = SurfChestSinglePlayerGui(title, player, rows, this)
    menu.init()
    return menu
}