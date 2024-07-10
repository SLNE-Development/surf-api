package dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets;

import dev.slne.surf.surfapi.bukkit.api.nms.SurfBukkitNmsBridge;
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.SurfBukkitNmsSpawnPackets;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public interface SurfBukkitNmsPacketBridges {

  SurfBukkitNmsSpawnPackets getSpawnPackets();

  PacketOperation createEmptyPacketOperation();

  static SurfBukkitNmsPacketBridges get() {
    return SurfBukkitNmsBridge.get().getPacketBridges();
  }
}
