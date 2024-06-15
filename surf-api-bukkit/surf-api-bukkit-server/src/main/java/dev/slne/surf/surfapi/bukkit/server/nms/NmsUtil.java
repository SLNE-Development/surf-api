package dev.slne.surf.surfapi.bukkit.server.nms;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display.BillboardConstraints;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_20_R3.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R3.util.CraftMagicNumbers;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Quaternionf;
import org.joml.Vector3f;

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

  default Vector3f toNms(org.spongepowered.math.vector.Vector3f vector) {
    return new Vector3f(vector.x(), vector.y(), vector.z());
  }

  default Quaternionf toNms(org.spongepowered.math.imaginary.Quaternionf quaternion) {
    return new Quaternionf(quaternion.x(), quaternion.y(), quaternion.z(), quaternion.w());
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
}
