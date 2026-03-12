package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.component.components

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.component.ViewContainerComponent
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.ViewRows
import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder

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