@file:OptIn(ExperimentalContracts::class)

package dev.slne.surf.surfapi.bukkit.api.inventory.dsl

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.pane.OutlinePane
import com.github.stefvanschie.inventoryframework.pane.Pane
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import com.github.stefvanschie.inventoryframework.pane.util.Slot
import dev.slne.surf.surfapi.bukkit.api.inventory.item.guiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.SubmitItemPane
import dev.slne.surf.surfapi.bukkit.api.inventory.types.SurfChestGui
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.jetbrains.annotations.Range
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@PaneMarker
class StaticPaneScope(slot: Slot, length: Int, height: Int) : StaticPane(slot, length, height)

fun SurfChestGui.drawOutline(
    slot: Slot,
    height: @Range(from = 1, to = 6) Int,
    length: @Range(from = 1, to = 9) Int = 9,
    init: @PaneMarker OutlinePane.() -> Unit
): OutlinePane {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    val pane = OutlinePane(slot, length, height, Pane.Priority.LOWEST)
    pane.init()
    addPane(pane)

    return pane
}

fun SurfChestGui.drawOutline(
    slot: Slot,
    height: @Range(from = 1, to = 6) Int,
    length: @Range(from = 1, to = 9) Int = 9,
    item: GuiItem = guiItem(Material.GRAY_STAINED_GLASS_PANE) { isCancelled = true }
) = drawOutline(slot, height, length) {
    addItem(item)
    setRepeat(true)
}

@OptIn(ExperimentalContracts::class)
fun SurfChestGui.drawOutlineRow(
    row: @Range(from = 0, to = 5) Int,
    length: @Range(from = 1, to = 9) Int = 9,
    init: @PaneMarker OutlinePane.() -> Unit
): OutlinePane {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return drawOutline(slot(0, row), 1, length, init)
}

fun SurfChestGui.drawOutlineRow(
    row: @Range(from = 0, to = 5) Int,
    length: @Range(from = 1, to = 9) Int = 9,
    item: GuiItem = guiItem(Material.GRAY_STAINED_GLASS_PANE) { isCancelled = true }
) = drawOutlineRow(row, length) {
    addItem(item)
    setRepeat(true)
}

fun SurfChestGui.makeStaticPane(
    slot: Slot,
    height: @Range(from = 1, to = 6) Int,
    length: @Range(from = 1, to = 9) Int,
    init: StaticPane.() -> Unit
): StaticPane {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    val pane = StaticPane(slot, length, height)
    pane.init()
    addPane(pane)

    return pane
}

fun SurfChestGui.makeSubmitItemPane(
    slot: Slot,
    length: @Range(from = 1, to = 6) Int,
    height: @Range(from = 1, to = 6) Int,
    filter: List<Material>,
    init: SubmitItemPane.() -> Unit = {}
): SubmitItemPane {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    val pane = SubmitItemPane(slot, length, height, filter)
    pane.init()
    addPane(pane)

    return pane
}

fun SurfChestGui.makeSubmitItemPane(
    slot: Slot,
    length: @Range(from = 1, to = 6) Int,
    height: @Range(from = 1, to = 6) Int,
    filter: (ItemStack) -> Boolean,
    init: SubmitItemPane.() -> Unit = {},
): SubmitItemPane {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    val pane = SubmitItemPane(slot, length, height, filter)
    pane.init()
    addPane(pane)

    return pane
}

fun SurfChestGui.addItem(
    slot: Slot,
    item: GuiItem
) = addPane(StaticPane(slot, 1, 1).apply { addItem(item, 0, 0) })

fun SurfChestGui.addItems(
    vararg items: Pair<Slot, GuiItem>
) = items.forEach { (slot, item) -> addItem(slot, item) }