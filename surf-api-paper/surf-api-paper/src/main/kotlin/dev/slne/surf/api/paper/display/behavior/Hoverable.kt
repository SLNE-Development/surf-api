package dev.slne.surf.api.paper.display.behavior

/**
 * Makes an element respond to hover interactions (player looking at it).
 */
class Hoverable(
    /** Called when the player starts looking at the element. */
    val onEnter: (InteractionContext) -> Unit = {},
    /** Called when the player stops looking at the element. */
    val onExit: (InteractionContext) -> Unit = {},
) : Behavior
