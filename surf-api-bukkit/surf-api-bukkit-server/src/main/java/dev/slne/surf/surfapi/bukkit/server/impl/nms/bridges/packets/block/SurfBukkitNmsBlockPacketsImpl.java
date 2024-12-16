package dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets.block;

import static com.google.common.base.Preconditions.checkNotNull;

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation;
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.block.SurfBukkitNmsBlockPackets;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets.PacketOperationImpl;
import dev.slne.surf.surfapi.bukkit.server.nms.NmsUtil;
import io.papermc.paper.math.BlockPosition;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import org.bukkit.block.data.BlockData;

@SuppressWarnings("UnstableApiUsage")
@ParametersAreNonnullByDefault
public final class SurfBukkitNmsBlockPacketsImpl implements SurfBukkitNmsBlockPackets, NmsUtil {

  @Override
  public PacketOperation updateBlockData(BlockPosition position, BlockData blockData) {
    checkNotNull(position, "position");
    checkNotNull(blockData, "blockData");
    return PacketOperationImpl.simple(
        player -> new ClientboundBlockUpdatePacket(toNms(position), toNms(blockData)));
  }

  @Override
  public PacketOperation resetBlock(BlockPosition position) {
    checkNotNull(position, "position");

    return PacketOperationImpl.simple(
        player -> new ClientboundBlockUpdatePacket(toNms(player).level(), toNms(position)));
  }
}
