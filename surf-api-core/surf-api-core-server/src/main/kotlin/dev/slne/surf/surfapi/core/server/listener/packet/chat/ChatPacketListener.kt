package dev.slne.surf.surfapi.core.server.listener.packet.chat

import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play

class ChatPacketListener(priority: PacketListenerPriority) : PacketListenerAbstract(priority) {
    override fun onPacketSend(event: PacketSendEvent) { // TODO: 02.03.2024 00:34 - packet events need to fix their wrapper
        if (event.packetType != Play.Server.SYSTEM_CHAT_MESSAGE) {
            return
        }

        //    final WrapperPlayServerSystemChatMessage packet = new WrapperPlayServerSystemChatMessage(event);
//    final String message = serializer.serialize(packet.getMessage());

//    System.out.println("Message: " + message);
//
//    if (!message.startsWith(ComponentMessage.COMPONENT_MESSAGE_PREFIX)) {
//      return;
//    }
//
//    System.out.println("Message starts with prefix");
//
//    final Component coloredMessage = GsonComponentSerializer.gson()
//        .deserialize(message.substring(ComponentMessage.COMPONENT_MESSAGE_PREFIX.length()));
//
//    packet.setMessage(coloredMessage);
    }
}
