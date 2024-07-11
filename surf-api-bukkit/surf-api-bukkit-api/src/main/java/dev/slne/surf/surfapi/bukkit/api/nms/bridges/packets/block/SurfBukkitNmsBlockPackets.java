package dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.block;

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation;
import io.papermc.paper.math.BlockPosition;
import javax.annotation.ParametersAreNonnullByDefault;
import org.bukkit.block.BlockState;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
@ParametersAreNonnullByDefault
public interface SurfBukkitNmsBlockPackets {

  PacketOperation updateBlockState(BlockPosition position, BlockState state)
}
