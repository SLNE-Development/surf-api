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
