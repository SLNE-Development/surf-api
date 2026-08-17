package dev.slne.surf.api.core.inventory.framework.internal

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.shared.api.util.InternalSurfApi

/**
 * Appends a single container component to this builder, surrounded by the shift glyphs that
 * position it and then reset the cursor for the next component.
 *
 * The pipeline is:
 * 1. Shift the cursor by [positionalShift] pixels.
 * 2. Emit [renderComponent].
 * 3. Shift the cursor back by `-(textureWidth + positionalShift)` pixels.
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
 * Builds the rendered inventory title string: every character is uppercased and inter-character
 * spacing glyphs are inserted between them.
 *
 * Unless the title is right-aligned, a leading spacing glyph is prepended as well.
 *
 * Shared by the per-platform `ViewContainerTitleComponent` implementations. This is internal
 * infrastructure — use the platform DSL instead.
 *
 * @param title the plain-text inventory title
 * @param charSpacing the pixel spacing to insert between each character
 * @param alignRight `true` when the title is right-aligned
 * @return the glyph-interleaved title string
 */
@InternalSurfApi
fun formatViewTitle(title: String, charSpacing: Int, alignRight: Boolean): String {
    val shifted = title.map { it.uppercase() }
        .joinToString(ShiftGlyphs.renderShift(charSpacing))

    return if (alignRight) {
        shifted
    } else {
        shifted.prependIndent(ShiftGlyphs.renderShift(charSpacing))
    }
}
