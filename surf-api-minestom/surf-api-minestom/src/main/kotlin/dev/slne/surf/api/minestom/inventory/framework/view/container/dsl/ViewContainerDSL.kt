package dev.slne.surf.api.minestom.inventory.framework.view.container.dsl

import dev.slne.surf.api.core.inventory.framework.internal.ViewSlotGeometry
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.minestom.inventory.framework.view.AbstractSurfViewRef
import dev.slne.surf.api.minestom.inventory.framework.view.container.component.ViewContainerComponent
import dev.slne.surf.api.minestom.inventory.framework.view.container.component.components.ViewBlockCellComponent
import dev.slne.surf.api.minestom.inventory.framework.view.container.component.components.ViewContainerRowTextComponent
import dev.slne.surf.api.minestom.inventory.framework.view.settings.SurfViewSettingsDefaults
import dev.slne.surf.api.minestom.inventory.framework.view.settings.ViewFontMetrics
import dev.slne.surf.api.minestom.inventory.framework.view.settings.ViewHeaderGeometry
import dev.slne.surf.api.minestom.inventory.framework.view.settings.ViewRows
import dev.slne.surf.api.minestom.inventory.framework.view.settings.align.TextAlignment
import it.unimi.dsi.fastutil.ints.IntCollection
import it.unimi.dsi.fastutil.ints.IntLists
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor

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
 * Sets the inventory title, replacing the one currently rendered.
 *
 * The title is rendered as-is in the font configured via
 * [SurfViewSettings.font][dev.slne.surf.api.minestom.inventory.framework.view.settings.SurfViewSettings.font]
 * - the client's own font by default - and positioned across the full width of the header texture,
 * which for the default [TextAlignment.CENTER] means centred above the slot grid. Colours and
 * decorations on [header] are kept; parts without a colour render white.
 *
 * ```kotlin
 * containerDefaults {
 *     header(text("Spieler-Statistiken", Colors.GOLD))
 * }
 * ```
 *
 * @param header the title component to render
 * @param alignment horizontal alignment of the title; defaults to the alignment the title
 *   currently rendered uses, or to the view's
 *   [headerTextAlignment][dev.slne.surf.api.minestom.inventory.framework.view.settings.SurfViewSettings.headerTextAlignment]
 *   if the container has no title yet
 */
context(context: ViewContainerModificationContext, ref: AbstractSurfViewRef)
fun header(
    header: Component,
    alignment: TextAlignment? = null,
) {
    ref.getRegisteredView().applyHeader(header, alignment)
}

/**
 * Sets the inventory title to the plain, white string [header].
 *
 * ```kotlin
 * containerDefaults {
 *     header("Spieler-Statistiken")
 * }
 * ```
 *
 * @param header the plain-text title to render
 * @param alignment horizontal alignment of the title; defaults to the alignment the title
 *   currently rendered uses, or to the view's
 *   [headerTextAlignment][dev.slne.surf.api.minestom.inventory.framework.view.settings.SurfViewSettings.headerTextAlignment]
 *   if the container has no title yet
 */
context(context: ViewContainerModificationContext, ref: AbstractSurfViewRef)
fun header(
    header: String,
    alignment: TextAlignment? = null,
) {
    header(Component.text(header), alignment)
}

/**
 * Sets the inventory title, building the component with a [SurfComponentBuilder] block.
 *
 * ```kotlin
 * containerDefaults {
 *     header {
 *         text("Shop ", Colors.GOLD)
 *         text("(Seite 1)", Colors.GRAY)
 *     }
 * }
 * ```
 *
 * @param alignment horizontal alignment of the title; defaults to the alignment the title
 *   currently rendered uses, or to the view's
 *   [headerTextAlignment][dev.slne.surf.api.minestom.inventory.framework.view.settings.SurfViewSettings.headerTextAlignment]
 *   if the container has no title yet
 * @param block builds the title component
 */
context(context: ViewContainerModificationContext, ref: AbstractSurfViewRef)
fun header(
    alignment: TextAlignment? = null,
    block: SurfComponentBuilder.() -> Unit,
) {
    header(buildText(block), alignment)
}

/**
 * Renders a line of [text] inside the slot [row], vertically centred in the row.
 *
 * The row comes from the font: the text is drawn in the view's
 * [rowFont][dev.slne.surf.api.minestom.inventory.framework.view.settings.SurfViewSettings.rowFont]
 * for [row], whose `ascent` in the resource pack centres it in that row - the server can only
 * position header glyphs horizontally. That horizontal position is computed from [alignment] within
 * the span of slot [columns], so a row can carry several independent texts.
 *
 * [text] is a full component: colours, gradients, decorations and hover events are kept, and parts
 * without a colour render white.
 *
 * ```kotlin
 * containerDefaults {
 *     rowText(1, text("Deine Statistiken", Colors.GOLD))
 *     rowText(3, text("Seite 1/4"), TextAlignment.RIGHT)
 *     rowText(5, text("Kategorie"), TextAlignment.LEFT, columns = 0..2)
 * }
 * ```
 *
 * @param row the one-based slot row to render in (1-6)
 * @param text the component to render, emitted as-is
 * @param alignment horizontal alignment within the [columns] span
 * @param columns the zero-based, inclusive span of slot columns to align in
 */
context(context: ViewContainerModificationContext, ref: AbstractSurfViewRef)
fun rowText(
    @ViewRows.Companion.Rows row: Int,
    text: Component,
    alignment: TextAlignment = SurfViewSettingsDefaults.DEFAULT_ROW_TEXT_ALIGNMENT,
    columns: IntRange = SurfViewSettingsDefaults.DEFAULT_ROW_TEXT_COLUMNS,
) {
    val settings = ref.getRegisteredView().settings
    require(row <= settings.rows.rows) {
        "Row $row is outside of this view, which only has ${settings.rows.rows} rows"
    }

    rowText(
        row = row,
        text = text,
        font = settings.rowFont(row),
        alignment = alignment,
        columns = columns,
        geometry = settings.headerGeometry,
        defaultColor = settings.headerTextColor,
        metrics = settings.rowFontMetrics
    )
}

/**
 * Renders the plain, white string [text] inside the slot [row], vertically centred in the row.
 *
 * @param row the one-based slot row to render in (1-6)
 * @param text the plain text to render, emitted as-is
 * @param alignment horizontal alignment within the [columns] span
 * @param columns the zero-based, inclusive span of slot columns to align in
 */
context(context: ViewContainerModificationContext, ref: AbstractSurfViewRef)
fun rowText(
    @ViewRows.Companion.Rows row: Int,
    text: String,
    alignment: TextAlignment = SurfViewSettingsDefaults.DEFAULT_ROW_TEXT_ALIGNMENT,
    columns: IntRange = SurfViewSettingsDefaults.DEFAULT_ROW_TEXT_COLUMNS,
) {
    rowText(row, Component.text(text), alignment, columns)
}

/**
 * Renders a line of text inside the slot [row], building the component with a
 * [SurfComponentBuilder] block.
 *
 * ```kotlin
 * containerDefaults {
 *     rowText(2, TextAlignment.LEFT, columns = 0..3) {
 *         text("Guthaben: ", Colors.GRAY)
 *         text("1.250", Colors.GREEN)
 *     }
 * }
 * ```
 *
 * @param row the one-based slot row to render in (1-6)
 * @param alignment horizontal alignment within the [columns] span
 * @param columns the zero-based, inclusive span of slot columns to align in
 * @param block builds the component to render
 */
context(context: ViewContainerModificationContext, ref: AbstractSurfViewRef)
fun rowText(
    @ViewRows.Companion.Rows row: Int,
    alignment: TextAlignment = SurfViewSettingsDefaults.DEFAULT_ROW_TEXT_ALIGNMENT,
    columns: IntRange = SurfViewSettingsDefaults.DEFAULT_ROW_TEXT_COLUMNS,
    block: SurfComponentBuilder.() -> Unit,
) {
    rowText(row, buildText(block), alignment, columns)
}

/**
 * Renders a line of [text] inside the slot [row] using an explicitly given row [font].
 *
 * Use this overload outside of a view's DSL scope, where the view's settings are not available as a
 * context receiver, or to render a row of text in a font other than the view's configured
 * [rowFont][dev.slne.surf.api.minestom.inventory.framework.view.settings.SurfViewSettings.rowFont].
 *
 * @param row the one-based slot row to render in (1-6)
 * @param text the component to render, emitted as-is
 * @param font the font whose `ascent` centres the text in [row]
 * @param alignment horizontal alignment within the [columns] span
 * @param columns the zero-based, inclusive span of slot columns to align in
 * @param geometry the header geometry describing the slot grid
 * @param defaultColor the colour applied to parts of [text] that do not set one
 * @param metrics the glyph metrics of [font]
 */
context(context: ViewContainerModificationContext)
fun rowText(
    @ViewRows.Companion.Rows row: Int,
    text: Component,
    font: Key,
    alignment: TextAlignment = SurfViewSettingsDefaults.DEFAULT_ROW_TEXT_ALIGNMENT,
    columns: IntRange = SurfViewSettingsDefaults.DEFAULT_ROW_TEXT_COLUMNS,
    geometry: ViewHeaderGeometry = SurfViewSettingsDefaults.DEFAULT_HEADER_GEOMETRY,
    defaultColor: TextColor = SurfViewSettingsDefaults.DEFAULT_HEADER_TEXT_COLOR,
    metrics: ViewFontMetrics = SurfViewSettingsDefaults.DEFAULT_ROW_FONT_METRICS,
) {
    addChild(
        ViewContainerRowTextComponent(
            row = row,
            text = text,
            font = font,
            alignment = alignment,
            columns = columns,
            geometry = geometry,
            defaultColor = defaultColor,
            metrics = metrics
        )
    )
}

/**
 * Renders the plain, white string [text] inside the slot [row] using an explicitly given row [font].
 *
 * @param row the one-based slot row to render in (1-6)
 * @param text the plain text to render, emitted as-is
 * @param font the font whose `ascent` centres the text in [row]
 * @param alignment horizontal alignment within the [columns] span
 * @param columns the zero-based, inclusive span of slot columns to align in
 * @param geometry the header geometry describing the slot grid
 * @param defaultColor the colour applied to parts of [text] that do not set one
 * @param metrics the glyph metrics of [font]
 */
context(context: ViewContainerModificationContext)
fun rowText(
    @ViewRows.Companion.Rows row: Int,
    text: String,
    font: Key,
    alignment: TextAlignment = SurfViewSettingsDefaults.DEFAULT_ROW_TEXT_ALIGNMENT,
    columns: IntRange = SurfViewSettingsDefaults.DEFAULT_ROW_TEXT_COLUMNS,
    geometry: ViewHeaderGeometry = SurfViewSettingsDefaults.DEFAULT_HEADER_GEOMETRY,
    defaultColor: TextColor = SurfViewSettingsDefaults.DEFAULT_HEADER_TEXT_COLOR,
    metrics: ViewFontMetrics = SurfViewSettingsDefaults.DEFAULT_ROW_FONT_METRICS,
) {
    rowText(row, Component.text(text), font, alignment, columns, geometry, defaultColor, metrics)
}

/**
 * Renders [text] in the slot [row], starting at the left edge of [column].
 *
 * Shorthand for a [rowText] left-aligned in the span from [column] to the last column.
 *
 * ```kotlin
 * containerDefaults {
 *     rowTextAt(2, 4, text("hier ->")) // starts above the middle slot of row 2
 * }
 * ```
 *
 * @param row the one-based slot row to render in (1-6)
 * @param column the zero-based slot column the text starts at (0-8)
 * @param text the component to render, emitted as-is
 */
context(context: ViewContainerModificationContext, ref: AbstractSurfViewRef)
fun rowTextAt(
    @ViewRows.Companion.Rows row: Int,
    column: Int,
    text: Component,
) {
    rowText(
        row = row,
        text = text,
        alignment = TextAlignment.LEFT,
        columns = column until ViewSlotGeometry.COLUMNS
    )
}

/**
 * Renders the plain, white string [text] in the slot [row], starting at the left edge of [column].
 *
 * @param row the one-based slot row to render in (1-6)
 * @param column the zero-based slot column the text starts at (0-8)
 * @param text the plain text to render, emitted as-is
 */
context(context: ViewContainerModificationContext, ref: AbstractSurfViewRef)
fun rowTextAt(
    @ViewRows.Companion.Rows row: Int,
    column: Int,
    text: String,
) {
    rowTextAt(row, column, Component.text(text))
}

/**
 * Removes every text rendered on the slot [row].
 *
 * @param row the one-based slot row to clear (1-6)
 */
context(context: ViewContainerModificationContext)
fun clearRowText(@ViewRows.Companion.Rows row: Int) {
    context.container.removeChildren { it is ViewContainerRowTextComponent && it.row == row }
}

/**
 * Removes every text rendered on any slot row, leaving the inventory title itself untouched.
 */
context(context: ViewContainerModificationContext)
fun clearRowTexts() {
    removeChildrenOfType<ViewContainerRowTextComponent>()
}

/**
 * Adds a [ViewBlockCellComponent] that visually blocks the cell at [column], [row].
 *
 * Blocking is **cosmetic only**: it draws a texture over the cell and changes nothing about the
 * slot itself, so items can still be placed into and taken out of a blocked slot exactly as before.
 *
 * ```kotlin
 * containerDefaults {
 *     blockCell(0, 1) // block column 0, row 1
 * }
 * ```
 *
 * @param column the zero-based column index (0-8)
 * @param row the one-based row index (1-6)
 * @param geometry the header geometry describing the slot grid; pass the view's
 *   [headerGeometry][dev.slne.surf.api.minestom.inventory.framework.view.settings.SurfViewSettings.headerGeometry]
 *   when it deviates from the default
 */
context(context: ViewContainerModificationContext)
fun blockCell(
    column: Int,
    @ViewRows.Companion.Rows row: Int,
    geometry: ViewHeaderGeometry = SurfViewSettingsDefaults.DEFAULT_HEADER_GEOMETRY,
) {
    addChild(ViewBlockCellComponent(column, row, geometry))
}

/**
 * Blocks the cell at the zero-based [slot] index, counted left-to-right and then top-to-bottom just
 * like the inventory's own slot numbering.
 *
 * ```kotlin
 * containerDefaults {
 *     blockSlot(13) // centre slot of row 2
 * }
 * ```
 *
 * @param slot the zero-based slot index (0-53)
 * @param geometry the header geometry describing the slot grid
 */
context(context: ViewContainerModificationContext)
fun blockSlot(
    slot: Int,
    geometry: ViewHeaderGeometry = SurfViewSettingsDefaults.DEFAULT_HEADER_GEOMETRY,
) {
    addChild(ViewBlockCellComponent.forSlot(slot, geometry))
}

/**
 * Blocks all cells in the given [row], skipping columns listed in [exemptColumns].
 *
 * ```kotlin
 * containerDefaults {
 *     blockRow(1, intArrayOf(4)) // block row 1 except column 4
 * }
 * ```
 *
 * @param row the one-based row index (1-6)
 * @param exemptColumns zero-based column indices to leave unblocked
 * @param geometry the header geometry describing the slot grid
 */
context(context: ViewContainerModificationContext)
fun blockRow(
    @ViewRows.Companion.Rows row: Int,
    exemptColumns: IntArray,
    geometry: ViewHeaderGeometry = SurfViewSettingsDefaults.DEFAULT_HEADER_GEOMETRY,
) {
    for (x in 0 until ViewSlotGeometry.COLUMNS) {
        if (x in exemptColumns) continue
        blockCell(x, row, geometry)
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
 * @param row the one-based row index (1-6)
 * @param exemptColumns zero-based column indices to leave unblocked; defaults to none
 * @param geometry the header geometry describing the slot grid
 */
context(context: ViewContainerModificationContext)
fun blockRow(
    @ViewRows.Companion.Rows row: Int,
    exemptColumns: IntCollection = IntLists.EMPTY_LIST,
    geometry: ViewHeaderGeometry = SurfViewSettingsDefaults.DEFAULT_HEADER_GEOMETRY,
) {
    for (x in 0 until ViewSlotGeometry.COLUMNS) {
        if (exemptColumns.contains(x)) continue
        blockCell(x, row, geometry)
    }
}

/**
 * Blocks all cells in the given [column], skipping rows listed in [exemptRows].
 *
 * @param column the zero-based column index (0-8)
 * @param exemptRows one-based row indices to leave unblocked
 * @param rows how many rows the view has; cells below it have no glyph to render
 * @param geometry the header geometry describing the slot grid
 */
context(context: ViewContainerModificationContext)
fun blockColumn(
    column: Int,
    exemptRows: IntArray,
    @ViewRows.Companion.Rows rows: Int = ViewSlotGeometry.MAX_ROWS,
    geometry: ViewHeaderGeometry = SurfViewSettingsDefaults.DEFAULT_HEADER_GEOMETRY,
) {
    for (row in 1..rows) {
        if (row in exemptRows) continue
        blockCell(column, row, geometry)
    }
}

/**
 * Blocks all cells in the given [column], skipping rows in [exemptRows].
 *
 * @param column the zero-based column index (0-8)
 * @param exemptRows one-based row indices to leave unblocked; defaults to none
 * @param rows how many rows the view has; cells below it have no glyph to render
 * @param geometry the header geometry describing the slot grid
 */
context(context: ViewContainerModificationContext)
fun blockColumn(
    column: Int,
    exemptRows: IntCollection = IntLists.EMPTY_LIST,
    @ViewRows.Companion.Rows rows: Int = ViewSlotGeometry.MAX_ROWS,
    geometry: ViewHeaderGeometry = SurfViewSettingsDefaults.DEFAULT_HEADER_GEOMETRY,
) {
    for (row in 1..rows) {
        if (exemptRows.contains(row)) continue
        blockCell(column, row, geometry)
    }
}

/**
 * Blocks every slot of a view with [rows] rows, skipping the slots listed in [exemptSlots].
 *
 * ```kotlin
 * containerDefaults {
 *     // every slot of a 5-row view is blocked, except the one in the middle
 *     blockAllSlots(ViewRows.FIVE, intSetOf(22))
 * }
 * ```
 *
 * @param rows how many rows the view has
 * @param exemptSlots zero-based slot indices to leave unblocked; defaults to none
 * @param geometry the header geometry describing the slot grid
 */
context(context: ViewContainerModificationContext)
fun blockAllSlots(
    rows: ViewRows,
    exemptSlots: IntCollection = IntLists.EMPTY_LIST,
    geometry: ViewHeaderGeometry = SurfViewSettingsDefaults.DEFAULT_HEADER_GEOMETRY,
) {
    blockAllSlots(rows.rows, exemptSlots, geometry)
}

/**
 * Blocks every slot of a view with [rows] rows, skipping the slots listed in [exemptSlots].
 *
 * @param rows how many rows the view has (1-6)
 * @param exemptSlots zero-based slot indices to leave unblocked; defaults to none
 * @param geometry the header geometry describing the slot grid
 */
context(context: ViewContainerModificationContext)
fun blockAllSlots(
    @ViewRows.Companion.Rows rows: Int,
    exemptSlots: IntCollection = IntLists.EMPTY_LIST,
    geometry: ViewHeaderGeometry = SurfViewSettingsDefaults.DEFAULT_HEADER_GEOMETRY,
) {
    for (slot in 0 until rows * ViewSlotGeometry.COLUMNS) {
        if (exemptSlots.contains(slot)) continue
        blockSlot(slot, geometry)
    }
}

/**
 * Removes the block overlay from the cell at [column], [row], if there is one.
 *
 * @param column the zero-based column index (0-8)
 * @param row the one-based row index (1-6)
 */
context(context: ViewContainerModificationContext)
fun unblockCell(column: Int, @ViewRows.Companion.Rows row: Int) {
    removeChild(ViewBlockCellComponent(column, row))
}

/**
 * Removes the block overlay from the cell at the zero-based [slot] index, if there is one.
 *
 * @param slot the zero-based slot index (0-53)
 */
context(context: ViewContainerModificationContext)
fun unblockSlot(slot: Int) {
    removeChild(ViewBlockCellComponent.forSlot(slot))
}

/**
 * Removes every block overlay from the container.
 */
context(context: ViewContainerModificationContext)
fun unblockAllSlots() {
    removeChildrenOfType<ViewBlockCellComponent>()
}

// -------------------------------------------------------------------------------------------------
// Binary compatibility
//
// The container DSL gained a `geometry` parameter - and `blockColumn` additionally a `rows`
// parameter - which changed the JVM signatures of the functions below, so plugins compiled against
// an older surf-api fail with a `NoSuchMethodError` at runtime. The shims keep the old signatures
// alive and delegate to the current functions with the parameters those callers never passed.
//
// They are hidden from Kotlin callers, so new code always resolves to the functions above. Remove
// them once every consumer has been rebuilt against this version.
// -------------------------------------------------------------------------------------------------

/** @suppress */
@Deprecated("Binary compatibility", ReplaceWith("blockCell(column, row)"), DeprecationLevel.HIDDEN)
context(context: ViewContainerModificationContext)
fun blockCell(column: Int, @ViewRows.Companion.Rows row: Int) {
    blockCell(column, row, SurfViewSettingsDefaults.DEFAULT_HEADER_GEOMETRY)
}

/** @suppress */
@Deprecated(
    "Binary compatibility",
    ReplaceWith("blockRow(row, exemptColumns)"),
    DeprecationLevel.HIDDEN
)
context(context: ViewContainerModificationContext)
fun blockRow(@ViewRows.Companion.Rows row: Int, exemptColumns: IntArray) {
    blockRow(row, exemptColumns, SurfViewSettingsDefaults.DEFAULT_HEADER_GEOMETRY)
}

/** @suppress */
@Deprecated(
    "Binary compatibility",
    ReplaceWith("blockRow(row, exemptColumns)"),
    DeprecationLevel.HIDDEN
)
context(context: ViewContainerModificationContext)
fun blockRow(
    @ViewRows.Companion.Rows row: Int,
    exemptColumns: IntCollection = IntLists.EMPTY_LIST,
) {
    blockRow(row, exemptColumns, SurfViewSettingsDefaults.DEFAULT_HEADER_GEOMETRY)
}

/** @suppress */
@Deprecated(
    "Binary compatibility",
    ReplaceWith("blockColumn(column, exemptRows)"),
    DeprecationLevel.HIDDEN
)
context(context: ViewContainerModificationContext)
fun blockColumn(column: Int, exemptRows: IntArray) {
    blockColumn(
        column = column,
        exemptRows = exemptRows,
        rows = ViewSlotGeometry.MAX_ROWS,
        geometry = SurfViewSettingsDefaults.DEFAULT_HEADER_GEOMETRY
    )
}

/** @suppress */
@Deprecated(
    "Binary compatibility",
    ReplaceWith("blockColumn(column, exemptRows)"),
    DeprecationLevel.HIDDEN
)
context(context: ViewContainerModificationContext)
fun blockColumn(column: Int, exemptRows: IntCollection = IntLists.EMPTY_LIST) {
    blockColumn(
        column = column,
        exemptRows = exemptRows,
        rows = ViewSlotGeometry.MAX_ROWS,
        geometry = SurfViewSettingsDefaults.DEFAULT_HEADER_GEOMETRY
    )
}

/** @suppress */
@Deprecated("Binary compatibility", ReplaceWith("header(header)"), DeprecationLevel.HIDDEN)
context(context: ViewContainerModificationContext, ref: AbstractSurfViewRef)
fun header(header: String) {
    header(header, null)
}

/**
 * No-op that keeps plugins compiled against the removed back-hint component loading.
 *
 * The navigation arrow the old `ViewContainerBackHintComponent` drew is part of the inventory
 * texture now; whether an outside click navigates back is decided solely by
 * [navigateBackOnOutsideClick][dev.slne.surf.api.minestom.inventory.framework.view.settings.SurfViewSettings.navigateBackOnOutsideClick].
 *
 * @suppress
 */
@Deprecated("The back hint is part of the inventory texture now", level = DeprecationLevel.HIDDEN)
context(context: ViewContainerModificationContext)
fun backHint() {
    // nothing to render anymore
}
