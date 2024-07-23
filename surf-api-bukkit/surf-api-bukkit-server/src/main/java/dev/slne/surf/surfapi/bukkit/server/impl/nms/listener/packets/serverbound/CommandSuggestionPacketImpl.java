package dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.serverbound;

import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.serverbound.CommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;

public final class CommandSuggestionPacketImpl extends NmsServerboundPacketImpl<ServerboundCommandSuggestionPacket> implements
    CommandSuggestionPacket {

  public CommandSuggestionPacketImpl(ServerboundCommandSuggestionPacket nmsPacket) {
    super(nmsPacket);
  }

  @Override
  public int completionId() {
    return getNmsPacket().getId();
  }

  @Override
  public String command() {
    return getNmsPacket().getCommand();
  }
}
