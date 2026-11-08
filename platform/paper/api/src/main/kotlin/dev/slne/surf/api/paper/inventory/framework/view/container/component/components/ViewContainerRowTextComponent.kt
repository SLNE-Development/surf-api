package dev.slne.surf.api.paper.inventory.framework.view.container.component.components

import dev.slne.surf.api.core.inventory.framework.internal.ViewSlotGeometry
import dev.slne.surf.api.core.inventory.framework.internal.layoutViewText
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.paper.inventory.framework.view.container.component.ViewContainerComponent
import dev.slne.surf.api.paper.inventory.framework.view.settings.SurfViewSettingsDefaults
import dev.slne.surf.api.paper.inventory.framework.view.settings.ViewFontMetrics
import dev.slne.surf.api.paper.inventory.framework.view.settings.ViewHeaderGeometry
import dev.slne.surf.api.paper.inventory.framework.view.settings.align.TextAlignment
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor

/**
 * A [ViewContainerComponent] that renders a line of text inside a slot [row] of the inventory,
 * vertically centred in the row.
 *
 * ### How a row is addressed
 *
 * The header is drawn as glyphs inside the inventory title, and a glyph's vertical position is
 * baked into the `ascent` of its font provider in the resource pack — the server cannot move it
 * down. The **row** therefore comes from the [font]: one font per slot row, each declaring the
 * `ascent` that centres its glyphs vertically in that row (`surf:menu_font_row_1` …
 * `surf:menu_font_row_6` by default, see [SurfViewSettingsDefaults.rowFont]). Only the
 * **horizontal** position is computed here, from [alignment] within the span of slot [columns].
 *
 * ### Styling and metrics
 *
 * [text] is a full Adventure [Component], so colours, gradients, decorations and hover events all
 * survive; parts without a colour are rendered in [defaultColor]. Because the text is positioned by
 * shifting the render cursor, its width has to be known up front, and that width comes from
 * [metrics] — which also applies the spacing, the casing and the space handling that font needs.
 * [metrics] therefore has to describe the font passed as [font]; the default
 * [SurfViewSettingsDefaults.DEFAULT_ROW_FONT_METRICS] describes the row fonts of the resource pack,
 * which copy the client font glyph sheets onto their row.
 *
 * ### Multiple texts per row
 *
 * Components are equal when they render the same component with the same font, alignment and
 * column span, so a row can carry several independent texts — for example one aligned left over the
 * first three columns and one aligned right over the last three.
 *
 * @param row the one-based slot row the text is rendered in (`1`–`6`); used for identity and
 *   validation, the actual vertical position comes from [font]
 * @param text the component to render
 * @param font the row font whose `ascent` centres the text in [row]
 * @param alignment horizontal [TextAlignment] within the [columns] span
 * @param columns the zero-based, inclusive span of slot columns the text is aligned in
 * @param geometry the header [ViewHeaderGeometry] describing the slot grid
 * @param defaultColor the colour applied to parts of [text] that do not set one
 * @param metrics the [ViewFontMetrics] of [font]
 */
class ViewContainerRowTextComponent(
    val row: Int,
    val text: Component,
    private val font: Key,
    val alignment: TextAlignment = SurfViewSettingsDefaults.DEFAULT_ROW_TEXT_ALIGNMENT,
    val columns: IntRange = SurfViewSettingsDefaults.DEFAULT_ROW_TEXT_COLUMNS,
    geometry: ViewHeaderGeometry = ViewHeaderGeometry.DEFAULT,
    private val defaultColor: TextColor = SurfViewSettingsDefaults.DEFAULT_HEADER_TEXT_COLOR,
    metrics: ViewFontMetrics = SurfViewSettingsDefaults.DEFAULT_ROW_FONT_METRICS,
) : ViewContainerComponent {

    /**
     * Renders the plain string [text] in [row].
     *
     * @param text the plain text to render
     */
    constructor(
        row: Int,
        text: String,
        font: Key,
        alignment: TextAlignment = SurfViewSettingsDefaults.DEFAULT_ROW_TEXT_ALIGNMENT,
        columns: IntRange = SurfViewSettingsDefaults.DEFAULT_ROW_TEXT_COLUMNS,
        geometry: ViewHeaderGeometry = ViewHeaderGeometry.DEFAULT,
        defaultColor: TextColor = SurfViewSettingsDefaults.DEFAULT_HEADER_TEXT_COLOR,
        metrics: ViewFontMetrics = SurfViewSettingsDefaults.DEFAULT_ROW_FONT_METRICS,
    ) : this(row, Component.text(text), font, alignment, columns, geometry, defaultColor, metrics)

    init {
        require(row in 1..ViewSlotGeometry.MAX_ROWS) {
            "Row must be in 1..${ViewSlotGeometry.MAX_ROWS}, was $row"
        }
        require(!columns.isEmpty()) { "A column span must cover at least one column" }
        require(columns.first >= 0 && columns.last < ViewSlotGeometry.COLUMNS) {
            "Columns must be in 0..${ViewSlotGeometry.COLUMNS - 1}, was $columns"
        }
    }

    private val laidOut = layoutViewText(
        component = text,
        charSpacing = metrics.charSpacing,
        uppercase = metrics.uppercase,
        spaceShift = metrics.spaceShift,
        advance = metrics::advance
    )

    override val textureWidth = laidOut.width

    override val positionalShift = alignment.calculateShift(
        textureWidth,
        geometry.columnSpanOptions(columns, metrics.charWidths, metrics.charSize, metrics.charSpacing)
    )

    /**
     * `false`: the width is derived from a font-metrics table, so a glyph the client renders at an
     * unexpected width would make it an estimate. Keeps the text from displacing the header
     * textures — see [ViewContainerComponent.hasExactWidth].
     */
    override val hasExactWidth = false

    override fun SurfComponentBuilder.renderComponent() {
        append(laidOut.component)
        font(font)
        colorIfAbsent(defaultColor)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ViewContainerRowTextComponent

        if (row != other.row) return false
        if (text != other.text) return false
        if (font != other.font) return false
        if (alignment != other.alignment) return false
        if (columns != other.columns) return false

        return true
    }

    override fun hashCode(): Int {
        var result = row
        result = 31 * result + text.hashCode()
        result = 31 * result + font.hashCode()
        result = 31 * result + alignment.hashCode()
        result = 31 * result + columns.hashCode()
        return result
    }
}
