package dev.slne.surf.api.minestom.inventory.framework.view.container.component.components

import dev.slne.surf.api.core.inventory.framework.internal.layoutViewText
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.minestom.inventory.framework.view.container.component.ViewContainerComponent
import dev.slne.surf.api.minestom.inventory.framework.view.settings.SurfViewSettingsDefaults
import dev.slne.surf.api.minestom.inventory.framework.view.settings.ViewFontMetrics
import dev.slne.surf.api.minestom.inventory.framework.view.settings.ViewHeaderGeometry
import dev.slne.surf.api.minestom.inventory.framework.view.settings.align.TextAlignment
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor

/**
 * A [ViewContainerComponent] that renders the inventory title at the top of the header.
 *
 * The [title] component keeps its colours, decorations and hover events; parts without a colour are
 * rendered in [defaultColor]. It is positioned by [textAlignment] across the full width of the
 * inventory as described by [geometry], which for the default [TextAlignment.CENTER] means centred
 * above the slot grid.
 *
 * Since the title is positioned by shifting the render cursor, its width has to be known up front,
 * and that width comes from [metrics]: the proportional widths of the client font by default, or the
 * cell width, spacing and casing of a fixed-width resource-pack font. [metrics] is also what applies
 * that spacing and casing to the text, so it has to describe the font passed as [font].
 *
 * @param title the inventory title, rendered as-is apart from what [metrics] transforms
 * @param font the Adventure [Key] of the font to render the title in; defaults to the vanilla
 *   font via [SurfViewSettingsDefaults.DEFAULT_HEADER_FONT]
 * @param textAlignment the [TextAlignment] controlling horizontal positioning
 * @param geometry the header [ViewHeaderGeometry] the title is aligned in
 * @param defaultColor the colour applied to parts of [title] that do not set one
 * @param metrics the [ViewFontMetrics] of [font]
 */
class ViewContainerTitleComponent(
    val title: Component,
    private val font: Key = SurfViewSettingsDefaults.DEFAULT_HEADER_FONT,
    val textAlignment: TextAlignment = SurfViewSettingsDefaults.DEFAULT_HEADER_ALIGNMENT,
    geometry: ViewHeaderGeometry = ViewHeaderGeometry.DEFAULT,
    private val defaultColor: TextColor = SurfViewSettingsDefaults.DEFAULT_HEADER_TEXT_COLOR,
    metrics: ViewFontMetrics = SurfViewSettingsDefaults.DEFAULT_HEADER_FONT_METRICS,
) : ViewContainerComponent {

    /**
     * Renders the plain string [title] as the inventory title.
     *
     * @param title the plain-text inventory title
     */
    constructor(
        title: String,
        font: Key = SurfViewSettingsDefaults.DEFAULT_HEADER_FONT,
        textAlignment: TextAlignment = SurfViewSettingsDefaults.DEFAULT_HEADER_ALIGNMENT,
        geometry: ViewHeaderGeometry = ViewHeaderGeometry.DEFAULT,
        defaultColor: TextColor = SurfViewSettingsDefaults.DEFAULT_HEADER_TEXT_COLOR,
        metrics: ViewFontMetrics = SurfViewSettingsDefaults.DEFAULT_HEADER_FONT_METRICS,
    ) : this(Component.text(title), font, textAlignment, geometry, defaultColor, metrics)

    private val laidOut = layoutViewText(
        component = title,
        charSpacing = metrics.charSpacing,
        uppercase = metrics.uppercase,
        spaceShift = metrics.spaceShift,
        advance = metrics::advance
    )

    override val textureWidth = laidOut.width

    override val positionalShift = textAlignment.calculateShift(
        textureWidth,
        geometry.titleOptions(metrics.charWidths, metrics.charSize, metrics.charSpacing)
    )

    /**
     * `false`: the width is derived from a font-metrics table, so a glyph the client renders at an
     * unexpected width would make it an estimate. Keeps the title from displacing the header
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

        other as ViewContainerTitleComponent

        if (title != other.title) return false
        if (font != other.font) return false
        if (textAlignment != other.textAlignment) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + font.hashCode()
        result = 31 * result + textAlignment.hashCode()
        return result
    }
}
