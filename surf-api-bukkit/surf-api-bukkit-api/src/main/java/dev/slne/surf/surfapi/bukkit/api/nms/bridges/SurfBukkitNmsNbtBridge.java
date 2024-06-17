package dev.slne.surf.surfapi.bukkit.api.nms.bridges;

import dev.slne.surf.surfapi.bukkit.api.nms.SurfBukkitNmsBridge;
import javax.annotation.ParametersAreNonnullByDefault;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
@ParametersAreNonnullByDefault
public interface SurfBukkitNmsNbtBridge {

  ItemStack makeItemStackEntityInvisible(ItemStack itemStack);

  String getNbtString(ItemStack itemStack, String key);

  static SurfBukkitNmsNbtBridge get() {
    return SurfBukkitNmsBridge.get().getNbtBridge();
  }
}
