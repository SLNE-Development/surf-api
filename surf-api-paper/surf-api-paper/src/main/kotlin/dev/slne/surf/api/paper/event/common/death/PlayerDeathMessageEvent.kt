package dev.slne.surf.api.paper.event.common.death

import dev.slne.surf.api.core.event.SurfCancellableEvent
import dev.slne.surf.api.core.event.SurfSyncEvent
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent

/**
 * Called when a death message for a player has been created and is about to be sent.
 *
 * The [message] can be modified to change the final death message that will be sent to
 * eligible receivers. Cancelling this event prevents the death message from being sent.
 *
 * This event is fired synchronously.
 *
 * @property player the player that died
 * @property message the death message that will be sent
 * @property lastDamageCause the last damage cause that resulted in the player's death
 */
data class PlayerDeathMessageEvent(
    val player: Player,
    var message: Component,
    val lastDamageCause: EntityDamageEvent.DamageCause,
) : SurfSyncEvent(), SurfCancellableEvent {
    override var isCancelled = false
}