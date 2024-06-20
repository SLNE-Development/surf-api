package dev.slne.surf.surfapi.core.server.listener.packet.chat;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class ChatPacketListener extends PacketListenerAbstract {

  private static final PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();

  public ChatPacketListener(PacketListenerPriority priority) {
    super(priority);
  }

  @Override
  public void onPacketSend(
      PacketSendEvent event) { // TODO: 02.03.2024 00:34 - packet events need to fix their wrapper
    if (event.getPacketType() != Server.SYSTEM_CHAT_MESSAGE) {
      return;
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
