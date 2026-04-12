package dev.slne.surf.api.paper.display.behavior

/**
 * Tracks the current interaction phase of an element.
 */
enum class ElementPhase {
    /** No interaction. */
    DEFAULT,

    /** Player is looking at this element. */
    HOVER,

    /** Player is clicking this element. */
    CLICK,
}
