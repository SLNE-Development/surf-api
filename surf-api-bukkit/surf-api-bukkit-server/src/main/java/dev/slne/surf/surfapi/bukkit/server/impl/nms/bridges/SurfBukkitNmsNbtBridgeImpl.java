package dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges;

import static com.google.common.base.Preconditions.checkNotNull;

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.SurfBukkitNmsNbtBridge;
import dev.slne.surf.surfapi.bukkit.server.nms.NmsUtil;
import javax.annotation.ParametersAreNonnullByDefault;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.component.CustomData;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@ParametersAreNonnullByDefault
public final class SurfBukkitNmsNbtBridgeImpl implements SurfBukkitNmsNbtBridge, NmsUtil {

  private static final ComponentLogger LOGGER = ComponentLogger.logger("SurfBukkitNmsNbtBridge");

  @Override
  public ItemStack makeItemStackEntityInvisible(ItemStack itemStack) {
    checkNotNull(itemStack, "itemStack");

    final net.minecraft.world.item.ItemStack nmsStack = toNms(itemStack);

    final CompoundTag nbt = new CompoundTag();
    nbt.putBoolean("Invisible", true);

    final DataComponentPatch patch = DataComponentPatch.builder()
        .set(DataComponents.ENTITY_DATA, CustomData.of(nbt))
        .build();

    nmsStack.applyComponents(patch);

    return toBukkit(nmsStack);
  }

  @Override
  @Deprecated
  public @NotNull String getNbtString(ItemStack itemStack, String key) {
    checkNotNull(itemStack, "itemStack");
    checkNotNull(key, "key");

    LOGGER.warn(
        "Using deprecated method getNbtString(ItemStack, String) in SurfBukkitNmsNbtBridgeImpl."
            + " ItemStacks now use DataComponents and nbt keys are not used. Please update your"
            + " code to use DataComponents instead."
    );

    final net.minecraft.world.item.ItemStack nmsStack = toNms(itemStack);
    return nmsStack.getComponents().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
        .getUnsafe().getString(key);
  }
}
