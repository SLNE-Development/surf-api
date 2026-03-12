package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.component.components

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.component.ViewContainerComponent
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.align.TextAlignment
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.align.TextAlignmentOptions
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.util.shift
import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.key.Key

internal class ViewContainerTitleComponent(
    title: String,
    private val font: Key,
    charSpacing: Int,
    textAlignment: TextAlignment
) : ViewContainerComponent {
    private val formattedTitle: String

    init {
        val shifted = title.map { it.uppercase() }
            .joinToString(shift(charSpacing))

        formattedTitle = if (textAlignment == TextAlignment.RIGHT) {
            shifted
        } else {
            shifted.prependIndent(shift(charSpacing))
        }
    }

    private val alignmentOptions = TextAlignmentOptions(
        leftShift = LEFT_SHIFT,
        padding = PADDING,
        containerWidth = CONTAINER_WIDTH,
        charSize = CHAR_SIZE,
        charSpacing = charSpacing
    )

    override val positionalShift = textAlignment.calculateShift(title, alignmentOptions)
    override val textureWidth =
        TextAlignment.calculateTextWidth(title, alignmentOptions) - 1 // -1 because of the last char spacing

    override fun SurfComponentBuilder.renderComponent() {
        text(formattedTitle)
        font(font)
        color(Colors.WHITE)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ViewContainerTitleComponent

        return formattedTitle == other.formattedTitle
    }

    override fun hashCode(): Int {
        return formattedTitle.hashCode()
    }


    companion object {
        const val LEFT_SHIFT = 31
        const val PADDING = 2
        const val CONTAINER_WIDTH = 100
        const val CHAR_SIZE = 9
        const val CHAR_SPACING = -1
    }
}