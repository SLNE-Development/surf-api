package dev.slne.surf.surfapi.bukkit.test.listener;

import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListener;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListenerResult;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.annotation.ServerboundListener;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.level.ServerPlayer;

public class ChatListener implements PacketListener {

  private static final ComponentLogger LOGGER = ComponentLogger.logger("TestChatListener");

  @ServerboundListener
  public PacketListenerResult onChatReceivePacket(ServerboundChatPacket packet,
      ServerPlayer player) {
    LOGGER.info("Player {} sent a chat message: {}", player.getName(), packet.message());
    return PacketListenerResult.CANCEL;
  }
}
