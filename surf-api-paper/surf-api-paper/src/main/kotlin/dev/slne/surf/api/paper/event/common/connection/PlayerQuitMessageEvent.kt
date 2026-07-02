package dev.slne.surf.api.paper.event.common.connection

import dev.slne.surf.api.core.event.SurfCancellableEvent
import dev.slne.surf.api.core.event.SurfSyncEvent
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

/**
 * Called when a quit message for a player has been created and is about to be sent.
 *
 * The [message] can be modified to change the final quit message that will be sent to
 * eligible receivers. Cancelling this event prevents the quit message from being sent.
 *
 * This event is fired synchronously.
 *
 * @property player the player that left the server
 * @property message the quit message that will be sent
 */
data class PlayerQuitMessageEvent(
    val player: Player,
    var message: Component,
) : SurfSyncEvent(), SurfCancellableEvent {
    override var isCancelled = false
}