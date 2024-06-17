package dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges;

import static com.google.common.base.Preconditions.checkNotNull;

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.SurfBukkitNmsNbtBridge;
import dev.slne.surf.surfapi.bukkit.server.nms.NmsUtil;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.world.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@ParametersAreNonnullByDefault
public final class SurfBukkitNmsNbtBridgeImpl implements SurfBukkitNmsNbtBridge, NmsUtil {

  @Override
  public ItemStack makeItemStackEntityInvisible(ItemStack itemStack) {
    checkNotNull(itemStack, "itemStack");

    final net.minecraft.world.item.ItemStack nmsStack = toNms(itemStack);
    nmsStack.getOrCreateTagElement(EntityType.ENTITY_TAG).putBoolean("Invisible", true);

    return toBukkit(nmsStack);
  }

  @Override
  public @NotNull String getNbtString(ItemStack itemStack, String key) {
    checkNotNull(itemStack, "itemStack");
    checkNotNull(key, "key");

    final net.minecraft.world.item.ItemStack nmsStack = toNms(itemStack);
    return nmsStack.getOrCreateTag().getString(key);
  }
}
