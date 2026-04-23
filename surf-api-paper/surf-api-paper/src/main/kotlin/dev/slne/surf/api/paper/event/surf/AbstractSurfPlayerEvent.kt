package dev.slne.surf.api.paper.event.surf

import dev.slne.surf.api.core.event.SurfAsyncEvent
import java.util.UUID

/**
 * Convenient base class for [SurfAsyncEvent]s that concern a single player,
 * identified by their UUID.
 *
 * Using the UUID rather than `org.bukkit.entity.Player` keeps the event
 * representable on platforms where the player has already disconnected by the
 * time a handler executes, and avoids handlers having to deal with `null`
 * players.
 *
 * @property playerUuid the UUID of the player this event refers to.
 */
abstract class AbstractSurfPlayerEvent(val playerUuid: UUID) : SurfAsyncEvent()
