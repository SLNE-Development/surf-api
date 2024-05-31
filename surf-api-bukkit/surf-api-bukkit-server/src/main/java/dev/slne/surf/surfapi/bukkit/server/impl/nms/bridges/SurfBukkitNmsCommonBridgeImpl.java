package dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges;

import static com.google.common.base.Preconditions.checkNotNull;

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.SurfBukkitNmsCommonBridge;
import dev.slne.surf.surfapi.bukkit.server.nms.NmsUtil;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.world.level.block.Block;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

@ParametersAreNonnullByDefault
public final class SurfBukkitNmsCommonBridgeImpl implements SurfBukkitNmsCommonBridge, NmsUtil {

  @Override
  public int getStateId(Material material) {
    checkNotNull(material, "material");

    return Block.getId(toNms(material).defaultBlockState());
  }

  @Override
  public int getStateId(BlockData blockData) {
    checkNotNull(blockData, "blockData");

    return Block.getId(toNms(blockData));
  }
}
