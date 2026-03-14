package dev.slne.surf.surfapi.bukkit.api.inventory.dsl

import com.github.stefvanschie.inventoryframework.pane.util.Slot
import org.jetbrains.annotations.Range

/**
 * Creates a [Slot] from column and row coordinates.
 *
 * Coordinates are zero-based: `x` ranges from `0` (leftmost) to `8` (rightmost),
 * and `y` ranges from `0` (top row) to `5` (bottom row of a 6-row chest).
 *
 * ```kotlin
 * val center = slot(4, 2) // column 5, row 3
 * ```
 *
 * @param x the column index (0–8)
 * @param y the row index (0–5)
 * @return the corresponding [Slot]
 */
fun slot(x: @Range(from = 0, to = 8) Int, y: @Range(from = 0, to = 5) Int) = Slot.fromXY(x, y)

/**
 * Creates a [Slot] from a linear inventory index.
 *
 * The index is zero-based and maps left-to-right, top-to-bottom, so index `0`
 * is the top-left slot and index `53` is the bottom-right slot of a 6-row chest.
 *
 * ```kotlin
 * val firstSlot = slot(0)
 * val lastSlot = slot(53)
 * ```
 *
 * @param index the linear slot index (0-based)
 * @return the corresponding [Slot]
 */
fun slot(index: Int) = Slot.fromIndex(index)