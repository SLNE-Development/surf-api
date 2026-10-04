package dev.slne.surf.api.minestom.server.impl.player

import dev.slne.surf.api.minestom.extension.PacketListenerManager
import dev.slne.surf.api.minestom.player.event.PlayerToggleFlightEvent
import net.minestom.server.entity.Player
import net.minestom.server.event.EventDispatcher
import net.minestom.server.listener.AbilitiesListener
import net.minestom.server.network.packet.client.play.ClientPlayerAbilitiesPacket
import net.minestom.server.network.packet.server.play.PlayerAbilitiesPacket
import kotlin.experimental.and

/**
 * Calls [PlayerToggleFlightEvent] before Minestom applies a player's flight toggle.
 */
internal object PlayerFlightListener {

    fun install() {
        PacketListenerManager.setPlayListener(ClientPlayerAbilitiesPacket::class.java, ::handle)
    }

    fun uninstall() {
        PacketListenerManager.setPlayListener(
            ClientPlayerAbilitiesPacket::class.java,
            AbilitiesListener::listener,
        )
    }

    private fun handle(packet: ClientPlayerAbilitiesPacket, player: Player) {
        val isFlying = packet.flags and PlayerAbilitiesPacket.FLAG_FLYING != 0.toByte()
        if (player.isAllowFlying && player.isFlying != isFlying) {
            val event = PlayerToggleFlightEvent(player, isFlying)
            EventDispatcher.call(event)
            if (event.isCancelled) {
                // Resends the abilities, which puts the client back into its previous state
                player.isFlying = player.isFlying
                return
            }
        }

        AbilitiesListener.listener(packet, player)
    }
}
