package dev.slne.surf.api.paper.display.behavior

/**
 * Makes an element respond to click interactions.
 */
class Clickable(
    /** Called when the element is left-clicked. */
    val onClick: (InteractionContext) -> Unit = {},
    /** Called when the element is right-clicked. */
    val onRightClick: (InteractionContext) -> Unit = {},
) : Behavior
