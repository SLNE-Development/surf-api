package dev.slne.surf.api.paper.inventory.framework.view.container.component.components

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.paper.inventory.framework.view.container.component.ViewContainerComponent
import dev.slne.surf.api.paper.inventory.framework.view.container.component.components.ViewContainerTitleComponent.Companion.CHAR_SIZE
import dev.slne.surf.api.paper.inventory.framework.view.container.component.components.ViewContainerTitleComponent.Companion.CHAR_SPACING
import dev.slne.surf.api.paper.inventory.framework.view.container.component.components.ViewContainerTitleComponent.Companion.CONTAINER_WIDTH
import dev.slne.surf.api.paper.inventory.framework.view.container.component.components.ViewContainerTitleComponent.Companion.LEFT_SHIFT
import dev.slne.surf.api.paper.inventory.framework.view.container.component.components.ViewContainerTitleComponent.Companion.PADDING
import dev.slne.surf.api.paper.inventory.framework.view.settings.align.TextAlignment
import dev.slne.surf.api.paper.inventory.framework.view.settings.align.TextAlignmentOptions
import dev.slne.surf.api.paper.inventory.framework.view.util.shift
import net.kyori.adventure.key.Key

/**
 * A [ViewContainerComponent] that renders the inventory title text using a custom font.
 *
 * The title string is converted to uppercase and inter-character spacing glyphs are inserted
 * between each letter using [shift]. The horizontal position is calculated from the
 * [textAlignment] so that the text is positioned correctly within the background texture.
 *
 * The component uses constants from its companion object to define the geometry of the
 * container area:
 * - [LEFT_SHIFT]: the base pixel offset from the left edge of the container
 * - [PADDING]: horizontal padding on each side
 * - [CONTAINER_WIDTH]: the usable pixel width of the title area
 * - [CHAR_SIZE]: the pixel width of a single character in the title font
 * - [CHAR_SPACING]: the inter-character spacing (negative = tighter)
 *
 * @param title the plain-text inventory title to render
 * @param font the Adventure [Key] identifying the resource-pack font to use
 * @param charSpacing the pixel spacing to insert between each character
 * @param textAlignment the [TextAlignment] controlling horizontal positioning
 */
class ViewContainerTitleComponent(
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
    override val textureWidth = TextAlignment.calculateTextWidth(title, alignmentOptions)

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
        /** The base pixel offset from the left edge of the container area. */
        const val LEFT_SHIFT = 31

        /** Horizontal padding on each side of the title text within the container. */
        const val PADDING = 2

        /** Total usable pixel width of the title container area. */
        const val CONTAINER_WIDTH = 100

        /** Pixel width of a single uppercase character in the title font. */
        const val CHAR_SIZE = 9

        /** Default inter-character spacing (negative = tighter). */
        const val CHAR_SPACING = -1
    }
}