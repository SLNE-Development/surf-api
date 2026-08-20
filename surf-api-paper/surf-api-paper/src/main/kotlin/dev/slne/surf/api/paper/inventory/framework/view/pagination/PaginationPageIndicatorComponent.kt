package dev.slne.surf.api.paper.inventory.framework.view.pagination

import dev.slne.surf.api.paper.inventory.framework.view.container.component.components.ViewContainerRowTextComponent
import dev.slne.surf.api.paper.inventory.framework.view.settings.SurfViewSettingsDefaults
import dev.slne.surf.api.paper.inventory.framework.view.settings.ViewFontMetrics
import dev.slne.surf.api.paper.inventory.framework.view.settings.ViewHeaderGeometry
import dev.slne.surf.api.paper.inventory.framework.view.settings.align.TextAlignment
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor

/**
 * The page counter a paginated view renders between its navigation buttons.
 *
 * A [ViewContainerRowTextComponent] centred in the free columns between the two buttons (which sit
 * in columns [PaginationButton.LEFT] and [PaginationButton.RIGHT]) of the button row. It only
 * exists as its own type so the view can replace the counter on every page switch without touching
 * any other row text the view may render in that row.
 *
 * @param row the one-based row the navigation buttons are in
 * @param text the counter component produced by the [PaginationPageIndicator]
 * @param font the row font whose `ascent` centres the counter in [row]
 * @param geometry the header [ViewHeaderGeometry] describing the slot grid
 * @param defaultColor the colour applied to parts of [text] that do not set one
 * @param metrics the glyph metrics of [font]
 */
internal class PaginationPageIndicatorComponent(
    row: Int,
    text: Component,
    font: Key,
    geometry: ViewHeaderGeometry = ViewHeaderGeometry.DEFAULT,
    defaultColor: TextColor = SurfViewSettingsDefaults.DEFAULT_HEADER_TEXT_COLOR,
    metrics: ViewFontMetrics = SurfViewSettingsDefaults.DEFAULT_ROW_FONT_METRICS,
) : ViewContainerRowTextComponent(
    row = row,
    text = text,
    font = font,
    alignment = TextAlignment.CENTER,
    columns = COLUMNS,
    geometry = geometry,
    defaultColor = defaultColor,
    metrics = metrics
) {
    companion object {
        /** The slot columns between the two navigation buttons. */
        val COLUMNS = (PaginationButton.LEFT.column + 1) until PaginationButton.RIGHT.column
    }
}
