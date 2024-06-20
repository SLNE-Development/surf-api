package dev.slne.surf.surfapi.bukkit.api.nms.bridges;

import dev.slne.surf.surfapi.bukkit.api.nms.SurfBukkitNmsBridge;
import javax.annotation.ParametersAreNonnullByDefault;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
@ParametersAreNonnullByDefault
public interface SurfBukkitNmsCommonBridge {

  int getStateId(Material material);

  int getStateId(BlockData blockData);

  int generateNextInventoryId(Player player);

  void addCompostable(Material material, float levelIncreaseChance);

  void removeCompostable(Material material);

  static SurfBukkitNmsCommonBridge get() {
    return SurfBukkitNmsBridge.get().getCommonBridge();
  }
}
