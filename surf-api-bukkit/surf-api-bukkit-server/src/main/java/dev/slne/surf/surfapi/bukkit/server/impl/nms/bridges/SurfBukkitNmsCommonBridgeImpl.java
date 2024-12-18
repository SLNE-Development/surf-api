package dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.SurfBukkitNmsCommonBridge;
import dev.slne.surf.surfapi.bukkit.server.nms.NmsUtil;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComposterBlock;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

@ParametersAreNonnullByDefault
public final class SurfBukkitNmsCommonBridgeImpl implements SurfBukkitNmsCommonBridge, NmsUtil {

  @SuppressWarnings("deprecation")
  @Override
  public int nextEntityId() {
    return Bukkit.getUnsafe().nextEntityId();
  }

  @Override
  public int getStateId(Material material) {
    checkNotNull(material, "material");

    return Block.getId(toNmsBlock(material).defaultBlockState());
  }

  @Override
  public int getStateId(BlockData blockData) {
    checkNotNull(blockData, "blockData");

    return Block.getId(toNms(blockData));
  }

  @Override
  public int generateNextInventoryId(Player player) {
    checkNotNull(player, "player");

    return toNms(player).nextContainerCounter();
  }

  @Override
  public void addCompostable(Material material, float levelIncreaseChance) {
    checkNotNull(material, "material");
    checkState(material.isItem(), "material must be an item");

    ComposterBlock.COMPOSTABLES.put(toNmsItem(material), levelIncreaseChance);
  }

  @Override
  public void removeCompostable(Material material) {
    checkNotNull(material, "material");
    checkState(material.isItem(), "material must be an item");

    ComposterBlock.COMPOSTABLES.removeFloat(toNmsItem(material));
  }
}
