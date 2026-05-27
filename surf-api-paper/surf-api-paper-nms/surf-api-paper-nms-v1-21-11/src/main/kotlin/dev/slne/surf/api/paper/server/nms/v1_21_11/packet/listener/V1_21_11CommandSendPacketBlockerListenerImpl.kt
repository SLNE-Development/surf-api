package dev.slne.surf.api.paper.server.nms.v1_21_11.packet.listener

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.common.CommandSendPacketBlockerListener
import dev.slne.surf.api.paper.packet.listener.listener.annotation.ClientboundListener
import net.minecraft.network.protocol.game.ClientboundCommandsPacket
import net.minecraft.server.level.ServerPlayer
import java.util.*

@OptIn(NmsUseWithCaution::class)
@Suppress("ClassName")
class V1_21_11CommandSendPacketBlockerListenerImpl(blockedPlayer: Set<UUID>) :
    CommandSendPacketBlockerListener(blockedPlayer) {

    @ClientboundListener
    fun onClientboundCommandsPacket(
        packet: ClientboundCommandsPacket,
        player: ServerPlayer
    ): ClientboundCommandsPacket? {
        if (blockedPlayer.contains(player.uuid)) return null
        return packet
    }
}