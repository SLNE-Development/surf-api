package dev.slne.surf.api.paper.event.surf

import dev.slne.surf.api.core.event.SurfAsyncEvent
import java.util.UUID

/** Base class for surf events that relate to a specific player. */
abstract class AbstractSurfPlayerEvent(
    /** UUID of the player this event concerns. */
    val playerUuid: UUID
) : SurfAsyncEvent()
