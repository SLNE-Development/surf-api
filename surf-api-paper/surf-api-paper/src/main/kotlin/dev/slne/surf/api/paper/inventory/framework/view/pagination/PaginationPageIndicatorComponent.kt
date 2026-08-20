package dev.slne.surf.api.paper.inventory.framework.view.pagination

import dev.slne.surf.api.core.inventory.framework.internal.TextAlignmentMath
import dev.slne.surf.api.core.inventory.framework.internal.layoutViewText
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.paper.inventory.framework.view.container.component.ViewContainerComponent
import dev.slne.surf.api.paper.inventory.framework.view.settings.SurfViewSettingsDefaults
import dev.slne.surf.api.paper.inventory.framework.view.settings.ViewFontMetrics
import dev.slne.surf.api.paper.inventory.framework.view.settings.ViewHeaderGeometry
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor

/**
 * The page counter a paginated view renders between its navigation buttons.
 *
 * It is centred in the free area of the button bar drawn by [PaginationButtonGlyphComponent] — the
 * gap between the two arrows, which sit at the ends of that bar — rather than on the slot grid. The
 * two centres are two pixels apart, so aligning on the slot grid would leave the counter looking
 * slightly off inside the bar. See [PaginationButtonGlyphComponent.counterAreaShift].
 *
 * The row itself comes from the [font], exactly like a row text: its `ascent` in the resource pack
 * is what puts the counter on the button row.
 *
 * @param row the one-based row the navigation buttons are in
 * @param text the counter component produced by the [PaginationPageIndicator]
 * @param font the row font whose `ascent` centres the counter in [row]
 * @param geometry the header [ViewHeaderGeometry] describing the slot grid the bar sits on
 * @param defaultColor the colour applied to parts of [text] that do not set one
 * @param metrics the [ViewFontMetrics] of [font]
 */
internal class PaginationPageIndicatorComponent(
    val row: Int,
    val text: Component,
    private val font: Key,
    geometry: ViewHeaderGeometry = ViewHeaderGeometry.DEFAULT,
    private val defaultColor: TextColor = SurfViewSettingsDefaults.DEFAULT_HEADER_TEXT_COLOR,
    metrics: ViewFontMetrics = SurfViewSettingsDefaults.DEFAULT_ROW_FONT_METRICS,
) : ViewContainerComponent {

    private val laidOut = layoutViewText(
        component = text,
        charSpacing = metrics.charSpacing,
        uppercase = metrics.uppercase,
        spaceShift = metrics.spaceShift,
        advance = metrics::advance
    )

    override val textureWidth = laidOut.width

    override val positionalShift = TextAlignmentMath.centerAlignedShift(
        textWidth = textureWidth,
        leftShift = PaginationButtonGlyphComponent.counterAreaShift(geometry),
        padding = 0,
        containerWidth = PaginationButtonGlyphComponent.COUNTER_AREA_WIDTH
    )

    /**
     * `false`: the width is derived from a font-metrics table, so a glyph the client renders at an
     * unexpected width would make it an estimate. Keeps the counter from displacing the header
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

        other as PaginationPageIndicatorComponent

        if (row != other.row) return false
        if (text != other.text) return false
        if (font != other.font) return false

        return true
    }

    override fun hashCode(): Int {
        var result = row
        result = 31 * result + text.hashCode()
        result = 31 * result + font.hashCode()
        return result
    }
}
