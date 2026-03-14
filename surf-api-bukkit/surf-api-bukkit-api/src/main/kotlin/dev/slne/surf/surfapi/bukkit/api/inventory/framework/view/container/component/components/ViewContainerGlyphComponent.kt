package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.component.components

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.component.ViewContainerComponent
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.ViewRows
import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder

/**
 * A [ViewContainerComponent] that renders the inventory background glyph for the given [rows].
 *
 * Each [ViewRows] value has a corresponding special-font character that renders the correct
 * background texture for that row count. The glyph is positioned with a fixed negative shift
 * of 48 pixels (moving the cursor to the left edge of the texture area) and has a texture
 * width of 223 pixels.
 *
 * @property rows the [ViewRows] value whose glyph character should be rendered
 */
internal class ViewContainerGlyphComponent(val rows: ViewRows): ViewContainerComponent {
    override val positionalShift = -48
    override val textureWidth = 223

    override fun SurfComponentBuilder.renderComponent() {
        text(rows.glyph)
        color(Colors.WHITE)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ViewContainerGlyphComponent

        return rows == other.rows
    }

    override fun hashCode(): Int {
        return rows.hashCode()
    }
}