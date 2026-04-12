package dev.slne.surf.api.paper.display.behavior

/**
 * Makes an element draggable.
 */
class Draggable(
    /** Called when a drag starts. */
    val onDragStart: (InteractionContext) -> Unit = {},
    /** Called during dragging with updated position. */
    val onDrag: (InteractionContext) -> Unit = {},
    /** Called when dragging ends. */
    val onDragEnd: (InteractionContext) -> Unit = {},
) : Behavior
