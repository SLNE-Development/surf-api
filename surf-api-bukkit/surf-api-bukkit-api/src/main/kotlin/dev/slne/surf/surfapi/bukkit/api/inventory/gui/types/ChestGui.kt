package dev.slne.surf.surfapi.bukkit.api.inventory.gui.types

import dev.slne.surf.surfapi.bukkit.api.inventory.InventoryBridge
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.Gui
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.MergedGui
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.NamedGui
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface ChestGui : NamedGui, MergedGui {
    val size: ChestGuiSize
    fun size(size: ChestGuiSize)

    override fun clone(): ChestGui = super.clone() as ChestGui

    enum class ChestGuiSize(val rows: Int) {
        ONE_ROW(1),
        TWO_ROWS(2),
        THREE_ROWS(3),
        FOUR_ROWS(4),
        FIVE_ROWS(5),
        SIX_ROWS(6);

        companion object {
            fun fromRows(rows: Int): ChestGuiSize? {
                return entries.find { it.rows == rows }
            }
        }
    }
}

fun chestMenu(
    size: ChestGui.ChestGuiSize = ChestGui.ChestGuiSize.SIX_ROWS,
    init: ChestGui.() -> Unit,
): ChestGui {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    val gui = InventoryBridge.instance.createChestGui(size)
    gui.init()
    return gui
}

fun Gui.childChestMenu(
    size: ChestGui.ChestGuiSize = ChestGui.ChestGuiSize.SIX_ROWS,
    init: ChestGui.() -> Unit,
): ChestGui {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    val gui = InventoryBridge.instance.createChestGui(size, this)
    gui.init()
    return gui
}