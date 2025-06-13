@file:OptIn(ExperimentalContracts::class)

package dev.slne.surf.surfapi.bukkit.api.inventory.dsl

import dev.slne.surf.surfapi.bukkit.api.inventory.gui.types.ChestGui
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.types.ChestSinglePlayerGui
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.utils.MergedGui
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.components.PagingButtons
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.panes.PaginatedPane
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.panes.StaticPane
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.Slot
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.jetbrains.annotations.Range
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@DslMarker
annotation class MenuMarker

fun MergedGui.pagingButtons(
    slot: Slot,
    paginatedPane: PaginatedPane,
    length: @Range(from = 1, to = 9) Int = 9,
    init: (@PaneMarker PagingButtons).() -> Unit,
): PagingButtons {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    return PagingButtons(slot, paginatedPane, length).apply {
        init()
        addPane(this)
    }
}

fun MergedGui.paginatedPane(
    slot: Slot,
    height: @Range(from = 1, to = 6) Int,
    length: @Range(from = 1, to = 9) Int = 9,
    init: (@PaneMarker PaginatedPane).() -> Unit,
): PaginatedPane {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    return PaginatedPane(slot, length, height).apply {
        init()
        addPane(this)
    }
}

fun MergedGui.staticPane(
    slot: Slot,
    height: @Range(from = 1, to = 6) Int,
    length: @Range(from = 1, to = 9) Int = 9,
    init: (@PaneMarker StaticPane).() -> Unit,
): StaticPane {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    return StaticPane(slot, length, height).apply {
        init()
        addPane(this)
    }
}

fun menu(
    title: Component,
    size: ChestGui.ChestGuiSize = ChestGui.ChestGuiSize.SIX_ROWS,
    init: @MenuMarker ChestGui.() -> Unit,
): ChestGui {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    return ChestGui(title, size).apply { init() }
}

fun playerMenu(
    title: Component,
    player: Player,
    size: ChestGui.ChestGuiSize = ChestGui.ChestGuiSize.SIX_ROWS,
    init: @MenuMarker ChestSinglePlayerGui.() -> Unit,
): ChestSinglePlayerGui {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    return ChestSinglePlayerGui(player, title, size).apply { init() }
}


fun ChestGui.childMenu(
    title: Component,
    size: ChestGui.ChestGuiSize = ChestGui.ChestGuiSize.SIX_ROWS,
    init: @MenuMarker ChestGui.() -> Unit,
): ChestGui {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    return ChestGui(title, size, this).apply { init() }
}

fun ChestSinglePlayerGui.childPlayerMenu(
    title: Component,
    size: ChestGui.ChestGuiSize = ChestGui.ChestGuiSize.SIX_ROWS,
    init: @MenuMarker ChestSinglePlayerGui.() -> Unit,
): ChestSinglePlayerGui {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    return ChestSinglePlayerGui(player, title, size, this).apply { init() }
}