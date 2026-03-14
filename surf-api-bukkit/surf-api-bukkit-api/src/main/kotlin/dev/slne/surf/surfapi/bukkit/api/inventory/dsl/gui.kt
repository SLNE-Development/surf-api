@file:OptIn(ExperimentalContracts::class)

package dev.slne.surf.surfapi.bukkit.api.inventory.dsl

import com.github.stefvanschie.inventoryframework.gui.type.util.MergedGui
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import com.github.stefvanschie.inventoryframework.pane.util.Slot
import dev.slne.surf.surfapi.bukkit.api.inventory.types.SurfChestGui
import dev.slne.surf.surfapi.bukkit.api.inventory.types.SurfChestSinglePlayerGui
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.jetbrains.annotations.Range
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * DSL marker annotation for GUI builder blocks.
 *
 * Annotating a type or class with `@MenuMarker` restricts implicit `this`
 * access within the DSL so that only members of the innermost receiver can
 * be called without qualification, preventing accidental outer-scope calls.
 *
 * @see dev.slne.surf.surfapi.bukkit.api.inventory.dsl.PaneMarker
 */
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@DslMarker
annotation class MenuMarker

/**
 * Adds a [StaticPane] to this [MergedGui] and configures it with the [init] block.
 *
 * The pane is created at [slot], spanning [length] columns and [height] rows,
 * and is immediately registered with the underlying GUI.
 *
 * ```kotlin
 * menu(Component.text("My Menu")) {
 *     staticPane(slot(0, 0), height = 2, length = 9) {
 *         item(slot(0, 0)) { click = { isCancelled = true } }
 *     }
 * }
 * ```
 *
 * @receiver the [MergedGui] (e.g. a [SurfChestGui]) to add the pane to
 * @param slot the top-left [Slot] position of the pane
 * @param height the number of rows the pane spans (1..6)
 * @param length the number of columns the pane spans (1..9); defaults to `9`
 * @param init configuration block applied to the new [StaticPane]
 * @see dev.slne.surf.surfapi.bukkit.api.inventory.dsl.PaneMarker
 */
fun MergedGui.staticPane(
    slot: Slot,
    height: @Range(from = 1, to = 6) Int,
    length: @Range(from = 1, to = 9) Int = 9,
    init: (@PaneMarker StaticPane).() -> Unit,
) {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    val pane = StaticPane(slot, length, height)
    pane.init()
    addPane(pane)
}

/**
 * Creates and configures a [SurfChestGui] (shared menu not bound to a specific player).
 *
 * The [init] block is called exactly once on the newly created GUI, which is returned
 * after configuration.
 *
 * ```kotlin
 * val gui = menu(Component.text("Shop"), rows = 4) {
 *     staticPane(slot(0, 0), height = 4) {
 *         item(slot(4, 1), ItemStack(Material.DIAMOND)) { click = { isCancelled = true } }
 *     }
 * }
 * gui.gui.show(player)
 * ```
 *
 * @param title the Adventure [Component] displayed in the inventory title bar
 * @param rows the number of inventory rows (2..6); defaults to `6`
 * @param init DSL configuration block applied to the [SurfChestGui]
 * @return the fully configured [SurfChestGui]
 * @see playerMenu
 * @see SurfChestGui.childMenu
 */
fun menu(
    title: Component,
    rows: @Range(from = 2, to = 6) Int = 6,
    init: @MenuMarker SurfChestGui.() -> Unit,
): SurfChestGui {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    val menu = SurfChestGui(title, rows)
    menu.init()
    return menu
}

/**
 * Creates and configures a [SurfChestSinglePlayerGui] bound to a specific [Player].
 *
 * The [init] block is called exactly once. The returned GUI can be shown by calling
 * [dev.slne.surf.surfapi.bukkit.api.inventory.SinglePlayerGui.open].
 *
 * ```kotlin
 * val gui = playerMenu(Component.text("Profile"), player, rows = 3) {
 *     staticPane(slot(0, 0), height = 1) {
 *         item(slot(4, 0)) { click = { isCancelled = true } }
 *     }
 * }
 * gui.open()
 * ```
 *
 * @param title the Adventure [Component] displayed in the inventory title bar
 * @param player the [Player] this GUI is bound to
 * @param rows the number of inventory rows (2..6); defaults to `6`
 * @param init DSL configuration block applied to the [SurfChestSinglePlayerGui]
 * @return the fully configured [SurfChestSinglePlayerGui]
 * @see menu
 * @see SurfChestSinglePlayerGui.childPlayerMenu
 */
fun playerMenu(
    title: Component,
    player: Player,
    rows: @Range(from = 2, to = 6) Int = 6,
    init: @MenuMarker SurfChestSinglePlayerGui.() -> Unit,
): SurfChestSinglePlayerGui {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    val menu = SurfChestSinglePlayerGui(title, player, rows)
    menu.init()
    return menu
}


/**
 * Creates a child [SurfChestGui] whose [SurfGui.parent] is set to this GUI.
 *
 * Child menus appear on top of the parent; calling
 * [SurfGui.backToParent] from within the child navigates back to this GUI.
 *
 * ```kotlin
 * val parent = menu(Component.text("Main")) { ... }
 * val child = parent.childMenu(Component.text("Sub"), rows = 3) {
 *     staticPane(slot(0, 0), height = 1) {
 *         item(slot(8, 0)) { click = { whoClicked.backToParent() } }
 *     }
 * }
 * child.gui.show(player)
 * ```
 *
 * @receiver the parent [SurfChestGui]
 * @param title the Adventure [Component] title for the child inventory
 * @param rows the number of rows (2..6) for the child inventory
 * @param init DSL configuration block applied to the new child [SurfChestGui]
 * @return the fully configured child [SurfChestGui]
 * @see menu
 * @see SurfGui.backToParent
 */
fun SurfChestGui.childMenu(
    title: Component,
    rows: @Range(from = 2, to = 6) Int,
    init: @MenuMarker SurfChestGui.() -> Unit,
): SurfChestGui {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    val menu = SurfChestGui(title, rows, this)
    menu.init()
    return menu
}

/**
 * Creates a child [SurfChestSinglePlayerGui] whose [SurfGui.parent] is set to this GUI
 * and whose [SinglePlayerGui.player] is inherited from the receiver.
 *
 * The child automatically carries the same player reference. Calling
 * [SurfGui.backToParent] from within the child navigates back to this GUI.
 *
 * ```kotlin
 * val parent = playerMenu(Component.text("Main"), player) { ... }
 * val child = parent.childPlayerMenu(Component.text("Settings"), rows = 3) {
 *     staticPane(slot(0, 0), height = 1) {
 *         item(slot(8, 0)) { click = { whoClicked.backToParent() } }
 *     }
 * }
 * child.open()
 * ```
 *
 * @receiver the parent [SurfChestSinglePlayerGui]
 * @param title the Adventure [Component] title for the child inventory
 * @param rows the number of rows (2..6) for the child inventory
 * @param init DSL configuration block applied to the new child [SurfChestSinglePlayerGui]
 * @return the fully configured child [SurfChestSinglePlayerGui]
 * @see playerMenu
 * @see SurfGui.backToParent
 */
fun SurfChestSinglePlayerGui.childPlayerMenu(
    title: Component,
    rows: @Range(from = 2, to = 6) Int,
    init: @MenuMarker SurfChestSinglePlayerGui.() -> Unit,
): SurfChestSinglePlayerGui {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    val menu = SurfChestSinglePlayerGui(title, player, rows, this)
    menu.init()
    return menu
}