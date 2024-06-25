package dev.slne.surf.surfapi.bukkit.api.nms.bridges;

import dev.slne.surf.surfapi.bukkit.api.nms.SurfBukkitNmsBridge;
import javax.annotation.ParametersAreNonnullByDefault;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
@ParametersAreNonnullByDefault
public interface SurfBukkitNmsNbtBridge {

  ItemStack makeItemStackEntityInvisible(ItemStack itemStack);

  /**
   * Get the NBT string from the item stack with the given key
   *
   * @param itemStack the item stack
   * @param key       the key
   * @return the NBT string
   * @deprecated Now only uses the nbt in the custom data components (item stacks now use data
   * components, and nbt should not be avoided)
   */
  @Deprecated
  String getNbtString(ItemStack itemStack, String key);

  static SurfBukkitNmsNbtBridge get() {
    return SurfBukkitNmsBridge.get().getNbtBridge();
  }
}
