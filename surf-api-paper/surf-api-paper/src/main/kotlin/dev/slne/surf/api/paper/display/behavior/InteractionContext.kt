package dev.slne.surf.api.paper.display.behavior

import dev.slne.surf.api.paper.display.element.Element
import java.util.UUID

/**
 * Context passed to behavior handlers during interactions.
 */
data class InteractionContext(
    /** UUID of the interacting player. */
    val playerId: UUID,
    /** The element being interacted with. */
    val element: Element,
    /** X pixel coordinate on the display where the interaction occurred. */
    val pixelX: Int,
    /** Y pixel coordinate on the display where the interaction occurred. */
    val pixelY: Int,
)
