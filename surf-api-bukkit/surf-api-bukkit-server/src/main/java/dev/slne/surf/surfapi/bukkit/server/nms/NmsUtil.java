package dev.slne.surf.surfapi.bukkit.server.nms;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_20_R3.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R3.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
@ParametersAreNonnullByDefault
public interface NmsUtil {

  default ServerPlayer toNms(Player player) {
    return ((CraftPlayer) player).getHandle();
  }

  default Block toNms(Material material) {
    return CraftMagicNumbers.getBlock(material);
  }

  default BlockState toNms(BlockData blockData) {
    return ((CraftBlockData) blockData).getState();
  }
}
