package dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.block;

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation;
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.SurfBukkitNmsPacketBridges;
import io.papermc.paper.math.BlockPosition;
import javax.annotation.ParametersAreNonnullByDefault;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@SuppressWarnings("UnstableApiUsage")
@NonExtendable
@ParametersAreNonnullByDefault
public interface SurfBukkitNmsBlockPackets {

  PacketOperation updateBlockData(BlockPosition position, BlockData blockData);

  /**
   * Reset the block at the given position to its original state in the players world.
   *
   * @param position the position of the block to reset
   * @return the packet operation
   */
  PacketOperation resetBlock(BlockPosition position);

  static SurfBukkitNmsBlockPackets get() {
    return SurfBukkitNmsPacketBridges.get().getBlockPackets();
  }
}
