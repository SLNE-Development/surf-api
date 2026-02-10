package dev.slne.surf.surfapi.core.server.listener.packet.chat

import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage
import dev.slne.surf.surfapi.core.api.messages.ComponentMessage
import dev.slne.surf.surfapi.core.api.messages.adventure.plain
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer

class ChatPacketListener(priority: PacketListenerPriority) : PacketListenerAbstract(priority) {
    override fun onPacketSend(event: PacketSendEvent) {
        if (event.packetType != Play.Server.SYSTEM_CHAT_MESSAGE) {
            return
        }

        val packet = WrapperPlayServerSystemChatMessage(event)
        val message = packet.message.plain()

        if (!message.startsWith(ComponentMessage.COMPONENT_MESSAGE_PREFIX)) {
            return
        }

        val coloredMessage = GsonComponentSerializer.gson()
            .deserialize(message.substring(ComponentMessage.COMPONENT_MESSAGE_PREFIX.length))

        packet.message = coloredMessage
        event.markForReEncode(true)
    }
}
