package dev.slne.surf.api.minestom.player

import net.minestom.server.entity.Player
import net.minestom.server.event.trait.PlayerEvent
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun Player.requireSurfPlayer(): SurfPlayer {
    contract {
        returns() implies (this@requireSurfPlayer is SurfPlayer)
    }

    require(this is SurfPlayer) { "Player ${this.username} is not a SurfPlayer" }
    return this
}

val PlayerEvent.surfPlayer get() = player.requireSurfPlayer()