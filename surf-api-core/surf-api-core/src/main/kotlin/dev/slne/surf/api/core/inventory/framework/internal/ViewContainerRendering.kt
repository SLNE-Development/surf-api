package dev.slne.surf.api.core.inventory.framework.internal

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import it.unimi.dsi.fastutil.ints.Int2IntMap
import it.unimi.dsi.fastutil.ints.Int2IntMaps

/**
 * Appends a single container component to this builder, surrounded by the shift glyphs that
 * position it and then reset the cursor for the next component.
 *
 * The pipeline is:
 * 1. Shift the cursor by [positionalShift] pixels.
 * 2. Emit [renderComponent].
 * 3. Shift the cursor back by `-(textureWidth + positionalShift)` pixels.
 *
 * Step 3 only returns the cursor to the container origin if [textureWidth] matches the number of
 * pixels [renderComponent] actually advanced it by. Components that cannot guarantee that must
 * report `hasExactWidth = false` so the platform `ViewContainer` renders them last.
 *
 * Shared by the per-platform `ViewContainer.render()` implementations. This is internal
 * infrastructure — use the platform DSL instead.
 *
 * @param positionalShift the pixel offset applied before rendering the component
 * @param textureWidth the width in pixels of the rendered texture
 * @param renderComponent emits the component's visual
 */
@InternalSurfApi
fun SurfComponentBuilder.appendShiftedComponent(
    positionalShift: Int,
    textureWidth: Int,
    renderComponent: SurfComponentBuilder.() -> Unit,
) {
    if (positionalShift != 0) {
        append { text(ShiftGlyphs.renderShift(positionalShift)) }
    }

    append { renderComponent() }

    val resetShift = -(textureWidth + positionalShift)
    if (resetShift != 0) {
        append { text(ShiftGlyphs.renderShift(resetShift)) }
    }
}

/**
 * An inventory title that has been laid out for rendering.
 *
 * @property text the glyph-interleaved string to emit
 * @property width the exact number of pixels [text] advances the render cursor by
 */
@InternalSurfApi
data class RenderedViewTitle(val text: String, val width: Int)

/**
 * Lays out the rendered inventory title: every character is uppercased and inter-character
 * spacing glyphs are inserted between them. Unless the title is right-aligned, a leading spacing
 * glyph is prepended as well.
 *
 * The returned [RenderedViewTitle.width] is measured on the string that is actually emitted, so it
 * accounts for the leading spacing glyph and for uppercasing that changes the glyph count (`ß`
 * uppercases to `SS`). Text is walked per Unicode **code point**, so surrogate pairs are neither
 * split apart nor counted twice.
 *
 * Shared by the per-platform `ViewContainerTitleComponent` implementations. This is internal
 * infrastructure — use the platform DSL instead.
 *
 * @param title the plain-text inventory title
 * @param charSize the pixel width of a single character in the title font
 * @param charSpacing the pixel spacing to insert between each character
 * @param alignRight `true` when the title is right-aligned
 * @param charWidths per-code-point width overrides for glyphs that are not [charSize] wide
 * @return the string to render together with its exact pixel width
 */
@InternalSurfApi
fun renderViewTitle(
    title: String,
    charSize: Int,
    charSpacing: Int,
    alignRight: Boolean,
    charWidths: Int2IntMap = Int2IntMaps.EMPTY_MAP,
): RenderedViewTitle {
    val uppercased = title.uppercase()
    if (uppercased.isEmpty()) return RenderedViewTitle("", 0)

    val spacing = ShiftGlyphs.renderShift(charSpacing)
    val text = buildString {
        if (!alignRight) append(spacing)

        var index = 0
        while (index < uppercased.length) {
            if (index > 0) append(spacing)

            val codePoint = uppercased.codePointAt(index)
            appendCodePoint(codePoint)
            index += Character.charCount(codePoint)
        }
    }

    val width = TextAlignmentMath.textWidth(uppercased, charSize, charSpacing, charWidths) +
            if (alignRight) 0 else charSpacing

    return RenderedViewTitle(text, width)
}
