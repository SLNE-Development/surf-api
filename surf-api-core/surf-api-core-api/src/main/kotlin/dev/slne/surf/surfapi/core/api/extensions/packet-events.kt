package dev.slne.surf.surfapi.core.api.extensions

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import dev.slne.surf.surfapi.core.api.surfCoreApi
import java.util.*

val packetEvents get() = PacketEvents.getAPI() ?: error("PacketEvents API is not yet initialized")

/**
 * Sends the current packet to the player associated with the provided UUID.
 * If no player is found for the specified UUID, the method returns without performing any action.
 *
 * @param uuid the UUID of the player to whom the packet will be sent
 */
fun PacketWrapper<*>.sendPacket(uuid: UUID) {
    val player = surfCoreApi.getPlayer(uuid) ?: return
    sendPacket(player)
}

/**
 * Sends the current packet to the specified player.
 *
 * @param player the platform-specific player object to whom the packet will be sent
 */
fun PacketWrapper<*>.sendPacket(player: Any) {
    packetEvents.playerManager.sendPacket(player, this)
}
