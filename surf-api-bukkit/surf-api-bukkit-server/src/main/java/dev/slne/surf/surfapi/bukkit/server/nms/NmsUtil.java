package dev.slne.surf.surfapi.bukkit.server.nms;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display.BillboardConstraints;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Internal
@ParametersAreNonnullByDefault
public interface NmsUtil {

  default ServerPlayer toNms(Player player) {
    return ((CraftPlayer) player).getHandle();
  }

  default Block toNmsBlock(Material material) {
    return CraftMagicNumbers.getBlock(material);
  }

  default Item toNmsItem(Material material) {
    return CraftMagicNumbers.getItem(material);
  }

  default BlockState toNms(BlockData blockData) {
    return ((CraftBlockData) blockData).getState();
  }

  default Vector3f toNms(@Nullable org.spongepowered.math.vector.Vector3f vector) {
    return vector == null ? null : new Vector3f(vector.x(), vector.y(), vector.z());
  }

  default Quaternionf toNms(@Nullable org.spongepowered.math.imaginary.Quaternionf quaternion) {
    return quaternion == null ? null : new Quaternionf(quaternion.x(), quaternion.y(), quaternion.z(), quaternion.w());
  }

  default BillboardConstraints toNms(Billboard billboard) {
    return BillboardConstraints.valueOf(billboard.name());
  }

  default ItemStack toNms(org.bukkit.inventory.ItemStack itemStack) {
    return CraftItemStack.asNMSCopy(itemStack);
  }

  default ItemDisplayContext toNms(ItemDisplayTransform itemDisplayTransform) {
    return ItemDisplayContext.BY_ID.apply(itemDisplayTransform.ordinal());
  }

  default org.bukkit.inventory.ItemStack toBukkit(ItemStack itemStack) {
    return CraftItemStack.asCraftMirror(itemStack);
  }
}
