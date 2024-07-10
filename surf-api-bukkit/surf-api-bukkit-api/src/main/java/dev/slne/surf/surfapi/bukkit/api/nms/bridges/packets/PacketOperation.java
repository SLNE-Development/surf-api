package dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public interface PacketOperation {

  void execute(Player player);

  PacketOperation add(PacketOperation operation);
  
  static PacketOperation start() {
    return SurfBukkitNmsPacketBridges.get().createEmptyPacketOperation();
  }
}
