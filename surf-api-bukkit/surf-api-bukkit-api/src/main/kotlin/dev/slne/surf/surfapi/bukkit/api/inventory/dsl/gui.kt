@file:OptIn(ExperimentalContracts::class)

package dev.slne.surf.surfapi.bukkit.api.inventory.dsl

import dev.slne.surf.surfapi.bukkit.api.inventory.gui.MergedGui
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.types.ChestGuiImpl
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.types.ChestSinglePlayerGui
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.components.PagingButtonsImpl
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.panes.PaginatedPaneImpl
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.panes.StaticPaneImpl
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
    paginatedPane: PaginatedPaneImpl,
    length: @Range(from = 1, to = 9) Int = 9,
    init: (@PaneMarker PagingButtonsImpl).() -> Unit,
): PagingButtonsImpl {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    return PagingButtonsImpl(slot, paginatedPane, length).apply {
        init()
        addPane(this)
    }
}

fun MergedGui.paginatedPane(
    slot: Slot,
    height: @Range(from = 1, to = 6) Int,
    length: @Range(from = 1, to = 9) Int = 9,
    init: (@PaneMarker PaginatedPaneImpl).() -> Unit,
): PaginatedPaneImpl {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    return PaginatedPaneImpl(slot, length, height).apply {
        init()
        addPane(this)
    }
}

fun MergedGui.staticPane(
    slot: Slot,
    height: @Range(from = 1, to = 6) Int,
    length: @Range(from = 1, to = 9) Int = 9,
    init: (@PaneMarker StaticPaneImpl).() -> Unit,
): StaticPaneImpl {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    return StaticPaneImpl(slot, length, height).apply {
        init()
        addPane(this)
    }
}

fun menu(
    title: Component,
    size: ChestGuiImpl.ChestGuiSize = ChestGuiImpl.ChestGuiSize.SIX_ROWS,
    init: @MenuMarker ChestGuiImpl.() -> Unit,
): ChestGuiImpl {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    return ChestGuiImpl(title, size).apply { init() }
}

fun playerMenu(
    title: Component,
    player: Player,
    size: ChestGuiImpl.ChestGuiSize = ChestGuiImpl.ChestGuiSize.SIX_ROWS,
    init: @MenuMarker ChestSinglePlayerGui.() -> Unit,
): ChestSinglePlayerGui {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    return ChestSinglePlayerGui(player, title, size).apply { init() }
}


fun ChestGuiImpl.childMenu(
    title: Component,
    size: ChestGuiImpl.ChestGuiSize = ChestGuiImpl.ChestGuiSize.SIX_ROWS,
    init: @MenuMarker ChestGuiImpl.() -> Unit,
): ChestGuiImpl {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    return ChestGuiImpl(title, size, this).apply { init() }
}

fun ChestSinglePlayerGui.childPlayerMenu(
    title: Component,
    size: ChestGuiImpl.ChestGuiSize = ChestGuiImpl.ChestGuiSize.SIX_ROWS,
    init: @MenuMarker ChestSinglePlayerGui.() -> Unit,
): ChestSinglePlayerGui {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    return ChestSinglePlayerGui(player, title, size, this).apply { init() }
}