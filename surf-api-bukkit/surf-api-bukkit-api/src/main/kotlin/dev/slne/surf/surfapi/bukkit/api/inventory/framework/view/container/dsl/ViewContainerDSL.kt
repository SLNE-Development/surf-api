package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.dsl

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.component.ViewContainerComponent
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.component.components.ViewBlockCellComponent
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.component.components.ViewContainerBackHintComponent
import it.unimi.dsi.fastutil.ints.IntCollection
import it.unimi.dsi.fastutil.ints.IntLists

/**
 * Adds a [ViewContainerComponent] to the container.
 *
 * Duplicates (by [equals]/[hashCode]) are silently ignored.
 *
 * @param component the component to add
 */
context(context: ViewContainerModificationContext)
fun addChild(component: ViewContainerComponent) {
    context.container.addChild(component)
}

/**
 * Removes a [ViewContainerComponent] from the container.
 *
 * @param component the component to remove
 */
context(context: ViewContainerModificationContext)
fun removeChild(component: ViewContainerComponent) {
    context.container.removeChild(component)
}

/**
 * Returns `true` if the container has at least one component of the given Java [clazz].
 *
 * @param clazz the Java class to check for
 * @return `true` if at least one matching component exists
 */
context(context: ViewContainerModificationContext)
fun <T : ViewContainerComponent> hasComponentOfType(clazz: Class<T>): Boolean {
    return context.container.hasComponentOfType(clazz)
}

/**
 * Returns `true` if the container has at least one component of type [T].
 *
 * @return `true` if at least one matching component exists
 */
context(context: ViewContainerModificationContext)
inline fun <reified T : ViewContainerComponent> hasComponentOfType(): Boolean {
    return context.container.hasComponentOfType<T>()
}

/**
 * Removes all components of the given Java [type] from the container.
 *
 * @param type the Java class of the components to remove
 */
context(context: ViewContainerModificationContext)
fun <T : ViewContainerComponent> removeChildrenOfType(type: Class<T>) {
    context.container.removeChildrenOfType(type)
}

/**
 * Removes all components of type [T] from the container.
 */
context(context: ViewContainerModificationContext)
inline fun <reified T : ViewContainerComponent> removeChildrenOfType() {
    context.container.removeChildrenOfType<T>()
}

/**
 * Adds a [ViewBlockCellComponent] that visually blocks the cell at [column], [row].
 *
 * ```kotlin
 * containerDefaults {
 *     blockCell(0, 1) // block column 0, row 1
 * }
 * ```
 *
 * @param column the zero-based column index (0–8)
 * @param row the one-based row index (1–6)
 */
context(context: ViewContainerModificationContext)
fun blockCell(column: Int, row: Int) {
    addChild(ViewBlockCellComponent(column, row))
}

/**
 * Blocks all cells in the given [row], skipping columns listed in [exemptColumns].
 *
 * ```kotlin
 * containerDefaults {
 *     blockRow(0, intArrayOf(4)) // block row 0 except column 4
 * }
 * ```
 *
 * @param row the one-based row index (1–6)
 * @param exemptColumns zero-based column indices to leave unblocked
 */
context(context: ViewContainerModificationContext)
fun blockRow(row: Int, exemptColumns: IntArray) {
    for (x in 0 until 9) {
        if (x in exemptColumns) continue
        blockCell(x, row)
    }
}

/**
 * Blocks all cells in the given [row], skipping columns in [exemptColumns].
 *
 * ```kotlin
 * containerDefaults {
 *     blockRow(5) // block all cells in row 5
 * }
 * ```
 *
 * @param row the one-based row index (1–6)
 * @param exemptColumns zero-based column indices to leave unblocked; defaults to none
 */
context(context: ViewContainerModificationContext)
fun blockRow(row: Int, exemptColumns: IntCollection = IntLists.EMPTY_LIST) {
    for (x in 0 until 9) {
        if (exemptColumns.contains(x)) continue
        blockCell(x, row)
    }
}

/**
 * Blocks all cells in the given [column], skipping rows listed in [exemptRows].
 *
 * @param column the zero-based column index (0–8)
 * @param exemptRows one-based row indices to leave unblocked
 */
context(context: ViewContainerModificationContext)
fun blockColumn(column: Int, exemptRows: IntArray) {
    for (y in 0 until 9) {
        if (y in exemptRows) continue
        blockCell(column, y)
    }
}

/**
 * Blocks all cells in the given [column], skipping rows in [exemptRows].
 *
 * @param column the zero-based column index (0–8)
 * @param exemptRows one-based row indices to leave unblocked; defaults to none
 */
context(context: ViewContainerModificationContext)
fun blockColumn(column: Int, exemptRows: IntCollection = IntLists.EMPTY_LIST) {
    for (y in 0 until 9) {
        if (exemptRows.contains(y)) continue
        blockCell(column, y)
    }
}

/**
 * Adds a [ViewContainerBackHintComponent] to the container.
 *
 * This renders a small navigation arrow in the inventory header, indicating to the player
 * that clicking outside the inventory will navigate back to the previous view.
 *
 * Added automatically when [SurfViewSettings.navigateBackOnOutsideClick][dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.SurfViewSettings.navigateBackOnOutsideClick]
 * is `true`.
 */
context(context: ViewContainerModificationContext)
fun backHint() {
    addChild(ViewContainerBackHintComponent)
}