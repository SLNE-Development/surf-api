package dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets;

import dev.slne.surf.surfapi.bukkit.api.nms.SurfBukkitNmsBridge;
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.block.SurfBukkitNmsBlockPackets;
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.SurfBukkitNmsSpawnPackets;
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.player.SurfBukkitNmsPlayerPackets;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public interface SurfBukkitNmsPacketBridges {

  SurfBukkitNmsSpawnPackets getSpawnPackets();

  PacketOperation createEmptyPacketOperation();

  SurfBukkitNmsBlockPackets getBlockPackets();

  SurfBukkitNmsPlayerPackets getPlayerPackets();

  static SurfBukkitNmsPacketBridges get() {
    return SurfBukkitNmsBridge.get().getPacketBridges();
  }
}
