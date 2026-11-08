package dev.slne.surf.surfapi.bukkit.test.command.subcommands.eventhandler.event

import dev.slne.surf.api.core.event.SurfAsyncEvent
import java.util.*

/**
 * A test async event used for testing the SurfEventBus async event handling.
 *
 * @param playerUuid The UUID of the player associated with this event.
 * @param message A test message payload.
 */
data class TestAsyncEvent(
    val playerUuid: UUID,
    val message: String,
) : SurfAsyncEvent()

