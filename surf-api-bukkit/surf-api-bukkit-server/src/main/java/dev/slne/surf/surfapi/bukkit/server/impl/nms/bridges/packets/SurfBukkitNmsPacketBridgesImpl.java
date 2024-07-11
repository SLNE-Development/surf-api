package dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets;

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.SurfBukkitNmsPacketBridges;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets.entity.SurfBukkitNmsSpawnPacketsImpl;

public final class SurfBukkitNmsPacketBridgesImpl implements SurfBukkitNmsPacketBridges {

  private final SurfBukkitNmsSpawnPacketsImpl spawnPackets;

  public SurfBukkitNmsPacketBridgesImpl() {
    this.spawnPackets = new SurfBukkitNmsSpawnPacketsImpl();
  }

  @Override
  public SurfBukkitNmsSpawnPacketsImpl getSpawnPackets() {
    return spawnPackets;
  }

  @Override
  public PacketOperationImpl createEmptyPacketOperation() {
    return PacketOperationImpl.empty();
  }
}
