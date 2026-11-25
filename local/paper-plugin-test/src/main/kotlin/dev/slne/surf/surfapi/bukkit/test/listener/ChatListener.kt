package dev.slne.surf.surfapi.bukkit.test.listener

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.packet.listener.listener.PacketListener
import dev.slne.surf.api.paper.packet.listener.listener.PacketListenerResult
import dev.slne.surf.api.paper.packet.listener.listener.annotation.ServerboundListener
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minecraft.network.protocol.game.ServerboundChatPacket
import net.minecraft.server.level.ServerPlayer

@OptIn(NmsUseWithCaution::class)
class ChatListener : PacketListener {

    @ServerboundListener
    fun onChatReceivePacket(
        packet: ServerboundChatPacket,
        player: ServerPlayer,
    ): PacketListenerResult {
        log.info("Player {} sent a chat message: {}", player.name, packet.message())

        return PacketListenerResult.CANCEL
    }

    private companion object {
        private val log = ComponentLogger.logger("TestChatListener")
    }
}
