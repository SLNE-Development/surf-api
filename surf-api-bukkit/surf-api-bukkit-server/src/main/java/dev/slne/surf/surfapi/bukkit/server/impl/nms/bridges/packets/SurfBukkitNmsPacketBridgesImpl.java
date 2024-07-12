package dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets;

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.SurfBukkitNmsPacketBridges;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets.block.SurfBukkitNmsBlockPacketsImpl;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets.entity.SurfBukkitNmsSpawnPacketsImpl;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets.player.SurfBukkitNmsPlayerPacketsImpl;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public final class SurfBukkitNmsPacketBridgesImpl implements SurfBukkitNmsPacketBridges {

  private final SurfBukkitNmsSpawnPacketsImpl spawnPackets = new SurfBukkitNmsSpawnPacketsImpl();
  private final SurfBukkitNmsBlockPacketsImpl blockPackets = new SurfBukkitNmsBlockPacketsImpl();
  private final SurfBukkitNmsPlayerPacketsImpl playerPackets = new SurfBukkitNmsPlayerPacketsImpl();

  @Override
  public PacketOperationImpl createEmptyPacketOperation() {
    return PacketOperationImpl.empty();
  }
}
