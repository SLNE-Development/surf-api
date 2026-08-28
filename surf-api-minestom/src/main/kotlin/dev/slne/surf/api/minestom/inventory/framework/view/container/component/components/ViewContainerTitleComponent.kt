package dev.slne.surf.api.minestom.inventory.framework.view.container.component.components

import dev.slne.surf.api.core.inventory.framework.internal.renderViewTitle
import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.minestom.inventory.framework.view.container.component.ViewContainerComponent
import dev.slne.surf.api.minestom.inventory.framework.view.settings.align.TextAlignment
import dev.slne.surf.api.minestom.inventory.framework.view.settings.align.TextAlignmentOptions
import it.unimi.dsi.fastutil.ints.Int2IntMap
import it.unimi.dsi.fastutil.ints.Int2IntMaps
import net.kyori.adventure.key.Key

/**
 * A [ViewContainerComponent] that renders the inventory title text using a custom font.
 *
 * The title string is converted to uppercase and inter-character spacing glyphs are inserted
 * between each letter using [renderViewTitle]. That same call reports the exact pixel width of the
 * string it produced, which is used both to position the text according to [textAlignment] and to
 * reset the render cursor afterwards — measuring the raw title instead would drift as soon as
 * uppercasing changes the glyph count (`ß` uppercases to `SS`) or the leading spacing glyph is
 * emitted.
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
 * @param charWidths per-code-point pixel width overrides for glyphs the [font] does not render at
 *   [CHAR_SIZE] pixels; see [TextAlignmentOptions.charWidths]
 */
class ViewContainerTitleComponent(
    title: String,
    private val font: Key,
    charSpacing: Int,
    textAlignment: TextAlignment,
    charWidths: Int2IntMap = Int2IntMaps.EMPTY_MAP
) : ViewContainerComponent {
    private val alignmentOptions = TextAlignmentOptions(
        leftShift = LEFT_SHIFT,
        padding = PADDING,
        containerWidth = CONTAINER_WIDTH,
        charSize = CHAR_SIZE,
        charSpacing = charSpacing,
        charWidths = charWidths
    )

    private val rendered = renderViewTitle(
        title = title,
        charSize = CHAR_SIZE,
        charSpacing = charSpacing,
        alignRight = textAlignment == TextAlignment.RIGHT,
        charWidths = charWidths
    )

    private val formattedTitle = rendered.text

    override val textureWidth = rendered.width
    override val positionalShift = textAlignment.calculateShift(rendered.width, alignmentOptions)

    /**
     * `false`: the width is derived from the configured font metrics, so a glyph the font renders
     * at an unexpected width would make it an estimate. Keeps the title from displacing the header
     * textures — see [ViewContainerComponent.hasExactWidth].
     */
    override val hasExactWidth = false

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
