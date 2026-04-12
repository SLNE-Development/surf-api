package dev.slne.surf.api.paper.display.behavior

/**
 * Makes an element respond to scroll interactions.
 */
class Scrollable(
    /** Called when the player scrolls while looking at the element. Direction: positive = up, negative = down. */
    val onScroll: (context: InteractionContext, direction: Int) -> Unit = { _, _ -> },
) : Behavior
