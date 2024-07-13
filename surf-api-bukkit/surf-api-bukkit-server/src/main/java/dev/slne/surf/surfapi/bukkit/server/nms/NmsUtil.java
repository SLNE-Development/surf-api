package dev.slne.surf.surfapi.bukkit.server.nms;

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.SurfBukkitNmsSpawnPackets.SignBlockUpdateSettings;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import java.util.stream.Stream;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display.BillboardConstraints;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@SuppressWarnings("UnstableApiUsage")
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

  default BlockState toNms(org.bukkit.block.BlockState blockState) {
    return ((CraftBlockState) blockState).getHandle();
  }

  default Quaternionf toNms(@Nullable org.spongepowered.math.imaginary.Quaternionf quaternion) {
    return quaternion == null ? null
        : new Quaternionf(quaternion.x(), quaternion.y(), quaternion.z(), quaternion.w());
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

  default BlockPos toNms(BlockPosition position) {
    return new BlockPos(position.blockX(), position.blockY(), position.blockZ());
  }

  default SignText toNms(SignBlockUpdateSettings.SignText signText) {
    final Component[] lines = Stream.of(signText.getLine1(), signText.getLine2(),
            signText.getLine3(), signText.getLine4())
        .map(PaperAdventure::asVanilla)
        .toArray(Component[]::new);

    return new SignText(lines, lines, DyeColor.BLACK, false);
  }

  default MenuType<?> toNms(InventoryType type) {
    return switch (type) {
      case ANVIL -> MenuType.ANVIL;
      case BEACON -> MenuType.BEACON;
      case BLAST_FURNACE -> MenuType.BLAST_FURNACE;
      case BREWING -> MenuType.BREWING_STAND;
      case CARTOGRAPHY -> MenuType.CARTOGRAPHY_TABLE;
      case CHEST -> MenuType.GENERIC_9x6;
      case DISPENSER, DROPPER -> MenuType.GENERIC_3x3;
      case ENCHANTING -> MenuType.ENCHANTMENT;
      case FURNACE -> MenuType.FURNACE;
      case GRINDSTONE -> MenuType.GRINDSTONE;
      case HOPPER -> MenuType.HOPPER;
      case LECTERN -> MenuType.LECTERN;
      case LOOM -> MenuType.LOOM;
      case MERCHANT -> MenuType.MERCHANT;
      case SHULKER_BOX -> MenuType.SHULKER_BOX;
      case SMOKER -> MenuType.SMOKER;
      case SMITHING -> MenuType.SMITHING;
      case STONECUTTER -> MenuType.STONECUTTER;
      case PLAYER -> MenuType.GENERIC_9x4;
      case CRAFTER -> MenuType.CRAFTER_3x3;
      case WORKBENCH -> MenuType.CRAFTING;
      case BARREL, CHISELED_BOOKSHELF, DECORATED_POT, JUKEBOX, COMPOSTER, ENDER_CHEST -> MenuType.GENERIC_9x3;
      case CREATIVE,CRAFTING -> throw new UnsupportedOperationException("Can't open a " + type + " inventory!");
      default -> throw new UnsupportedOperationException("Unknown inventory type: " + type);
    };
  }

  default BlockPosition toBukkit(BlockPos position) {
    return Position.block(position.getX(), position.getY(), position.getZ());
  }

  default net.kyori.adventure.text.Component[] toBukkit(Component[] components) {
    return Stream.of(components)
        .map(PaperAdventure::asAdventure)
        .toArray(net.kyori.adventure.text.Component[]::new);
  }
}
