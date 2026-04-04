package dev.slne.surf.api.paper.inventory.framework.view.container.component

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder

/**
 * A component that can be added to a [ViewContainer] and rendered as part of the
 * inventory's custom title.
 *
 * Each component describes a visual element (e.g. a background glyph, a title text, or a
 * navigation arrow) that is overlaid on the inventory header using Adventure's font system.
 *
 * The rendering pipeline works as follows:
 * 1. Shift the cursor by [positionalShift] pixels.
 * 2. Call [renderComponent] to emit the component's visual.
 * 3. Shift the cursor back by `-(textureWidth + positionalShift)` pixels to reset the position
 *    for the next component.
 *
 * All implementations must correctly override [equals] and [hashCode] so that
 * [CopyOnWriteArrayList.addIfAbsent][java.util.concurrent.CopyOnWriteArrayList.addIfAbsent]
 * works as expected in [ViewContainer].
 *
 * @property positionalShift the pixel offset applied before rendering the component
 * @property textureWidth the width in pixels of the rendered texture; used to calculate the reset shift
 * @see ViewContainer
 */
interface ViewContainerComponent {
    val positionalShift: Int
    val textureWidth: Int

    /**
     * Emits this component's visual into the given [SurfComponentBuilder].
     *
     * Called by [ViewContainer] as part of the [ViewContainer.render] pipeline.
     * Implementations should append the appropriate font glyphs or text to the builder.
     *
     * @receiver the [SurfComponentBuilder] to emit the component into
     */
    fun SurfComponentBuilder.renderComponent()

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}