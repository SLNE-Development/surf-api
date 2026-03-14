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

/**
 * A [StaticPane] subclass tagged with [@PaneMarker][PaneMarker] for use in DSL
 * pane configuration blocks.
 *
 * [StaticPaneScope] is not created directly; it is used as a DSL scope type to
 * restrict implicit receiver access within pane builder lambdas.
 *
 * @param slot the top-left position of the pane
 * @param length the number of columns the pane spans
 * @param height the number of rows the pane spans
 */
@PaneMarker
class StaticPaneScope(slot: Slot, length: Int, height: Int) : StaticPane(slot, length, height)

/**
 * Creates an [OutlinePane] at the given position with the given dimensions, configures it
 * with [init], and adds it to this [SurfChestGui] at [Pane.Priority.LOWEST].
 *
 * ```kotlin
 * menu(Component.text("Outlined")) {
 *     drawOutline(slot(0, 0), height = 4, length = 9) {
 *         addItem(guiItem(Material.BLACK_STAINED_GLASS_PANE) { isCancelled = true })
 *         setRepeat(true)
 *     }
 * }
 * ```
 *
 * @receiver the [SurfChestGui] to add the outline to
 * @param slot the top-left [Slot] of the outline pane
 * @param height the number of rows the outline spans (1..6)
 * @param length the number of columns the outline spans (1..9); defaults to `9`
 * @param init configuration block applied to the [OutlinePane]
 * @return the created and configured [OutlinePane]
 * @see drawOutlineRow
 */
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

/**
 * Convenience overload of [drawOutline] that fills the outline with a repeating [item].
 *
 * Defaults to a gray stained-glass pane with click cancellation enabled.
 *
 * ```kotlin
 * menu(Component.text("Framed")) {
 *     drawOutline(slot(0, 0), height = 3)
 * }
 * ```
 *
 * @receiver the [SurfChestGui] to add the outline to
 * @param slot the top-left [Slot] of the outline pane
 * @param height the number of rows the outline spans (1..6)
 * @param length the number of columns (1..9); defaults to `9`
 * @param item the [GuiItem] to repeat along the outline; defaults to a gray stained-glass pane
 * @return the created [OutlinePane]
 * @see drawOutline
 */
fun SurfChestGui.drawOutline(
    slot: Slot,
    height: @Range(from = 1, to = 6) Int,
    length: @Range(from = 1, to = 9) Int = 9,
    item: GuiItem = guiItem(Material.GRAY_STAINED_GLASS_PANE) { isCancelled = true }
) = drawOutline(slot, height, length) {
    addItem(item)
    setRepeat(true)
}

/**
 * Creates a single-row [OutlinePane] spanning the given [row] and configures it with [init].
 *
 * A convenience wrapper around [drawOutline] that always uses a height of `1`
 * and positions the pane at column `0` of the specified row.
 *
 * ```kotlin
 * menu(Component.text("Row Outline")) {
 *     drawOutlineRow(0) {
 *         addItem(guiItem(Material.WHITE_STAINED_GLASS_PANE) { isCancelled = true })
 *         setRepeat(true)
 *     }
 * }
 * ```
 *
 * @receiver the [SurfChestGui] to add the row outline to
 * @param row the zero-based row index (0..5)
 * @param length the number of columns to span (1..9); defaults to `9`
 * @param init configuration block applied to the [OutlinePane]
 * @return the created [OutlinePane]
 * @see drawOutlineRow
 * @see drawOutline
 */
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

/**
 * Convenience overload of [drawOutlineRow] that fills the row with a repeating [item].
 *
 * Defaults to a gray stained-glass pane with click cancellation.
 *
 * ```kotlin
 * menu(Component.text("Bordered")) {
 *     drawOutlineRow(0)  // top border
 *     drawOutlineRow(5)  // bottom border
 * }
 * ```
 *
 * @receiver the [SurfChestGui] to add the row outline to
 * @param row the zero-based row index (0..5)
 * @param length the number of columns to span (1..9); defaults to `9`
 * @param item the [GuiItem] to repeat in the row; defaults to a gray stained-glass pane
 * @return the created [OutlinePane]
 * @see drawOutlineRow
 */
fun SurfChestGui.drawOutlineRow(
    row: @Range(from = 0, to = 5) Int,
    length: @Range(from = 1, to = 9) Int = 9,
    item: GuiItem = guiItem(Material.GRAY_STAINED_GLASS_PANE) { isCancelled = true }
) = drawOutlineRow(row, length) {
    addItem(item)
    setRepeat(true)
}

/**
 * Creates a [StaticPane] at the given position with the given dimensions, configures it
 * with [init], and registers it with this [SurfChestGui].
 *
 * ```kotlin
 * menu(Component.text("Items")) {
 *     makeStaticPane(slot(1, 1), height = 3, length = 7) {
 *         addItem(guiItem(Material.APPLE) { isCancelled = true }, 0, 0)
 *     }
 * }
 * ```
 *
 * @receiver the [SurfChestGui] to add the pane to
 * @param slot the top-left [Slot] of the pane
 * @param height the number of rows the pane spans (1..6)
 * @param length the number of columns the pane spans (1..9)
 * @param init configuration block applied to the [StaticPane]
 * @return the created and configured [StaticPane]
 */
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

/**
 * Creates a [SubmitItemPane] with a material whitelist filter and registers it with this GUI.
 *
 * The pane allows players to place items matching any [Material] in [filter] into the
 * designated slots. Placed items are tracked in [SubmitItemPane.submittedItems].
 *
 * ```kotlin
 * menu(Component.text("Anvil Input")) {
 *     makeSubmitItemPane(slot(2, 1), length = 5, height = 2,
 *         filter = listOf(Material.DIAMOND, Material.GOLD_INGOT)
 *     ) {
 *         // additional pane configuration
 *     }
 * }
 * ```
 *
 * @receiver the [SurfChestGui] to add the pane to
 * @param slot the top-left [Slot] of the submit pane
 * @param length the number of columns the pane spans (1..6)
 * @param height the number of rows the pane spans (1..6)
 * @param filter list of [Material]s the pane accepts; items of other materials are rejected
 * @param init optional configuration block applied to the [SubmitItemPane]
 * @return the created [SubmitItemPane]
 * @see SubmitItemPane
 */
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

/**
 * Creates a [SubmitItemPane] with a predicate filter and registers it with this GUI.
 *
 * The [filter] lambda is called for each [ItemStack] a player tries to place.
 * Only items for which the predicate returns `true` are accepted.
 *
 * ```kotlin
 * menu(Component.text("Enchanted Input")) {
 *     makeSubmitItemPane(slot(2, 1), length = 5, height = 2,
 *         filter = { it.enchantments.isNotEmpty() }
 *     )
 * }
 * ```
 *
 * @receiver the [SurfChestGui] to add the pane to
 * @param slot the top-left [Slot] of the submit pane
 * @param length the number of columns the pane spans (1..6)
 * @param height the number of rows the pane spans (1..6)
 * @param filter predicate applied to each candidate [ItemStack]; must return `true` to allow placement
 * @param init optional configuration block applied to the [SubmitItemPane]
 * @return the created [SubmitItemPane]
 * @see SubmitItemPane
 */
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

/**
 * Adds a single [GuiItem] to this [SurfChestGui] at the given [slot].
 *
 * Internally creates a 1×1 [StaticPane] and immediately registers it with the GUI.
 *
 * ```kotlin
 * menu(Component.text("Single Item")) {
 *     addItem(slot(4, 2), guiItem(Material.DIAMOND) { isCancelled = true })
 * }
 * ```
 *
 * @receiver the [SurfChestGui] to add the item to
 * @param slot the [Slot] where the item should appear
 * @param item the [GuiItem] to display
 * @see addItems
 */
fun SurfChestGui.addItem(
    slot: Slot,
    item: GuiItem
) = addPane(StaticPane(slot, 1, 1).apply { addItem(item, 0, 0) })

/**
 * Adds multiple [GuiItem]s to this [SurfChestGui] at their respective slots.
 *
 * Each pair maps a [Slot] to a [GuiItem]. The items are added in the order they are provided.
 *
 * ```kotlin
 * menu(Component.text("Multi Items")) {
 *     addItems(
 *         slot(0, 0) to guiItem(Material.APPLE) { isCancelled = true },
 *         slot(8, 5) to guiItem(Material.GOLD_INGOT) { isCancelled = true }
 *     )
 * }
 * ```
 *
 * @receiver the [SurfChestGui] to add the items to
 * @param items the [Slot]-to-[GuiItem] pairs to add
 * @see addItem
 */
fun SurfChestGui.addItems(
    vararg items: Pair<Slot, GuiItem>
) = items.forEach { (slot, item) -> addItem(slot, item) }