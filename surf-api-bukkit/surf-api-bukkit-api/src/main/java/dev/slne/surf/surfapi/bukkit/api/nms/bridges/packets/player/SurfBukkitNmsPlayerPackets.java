package dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.player;

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation;
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.SurfBukkitNmsPacketBridges;
import io.papermc.paper.math.BlockPosition;
import javax.annotation.ParametersAreNonnullByDefault;
import net.kyori.adventure.text.Component;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
@ParametersAreNonnullByDefault
public interface SurfBukkitNmsPlayerPackets {

  PacketOperation openSignEditor(BlockPosition position, boolean frontSide);

  PacketOperation openInventory(int syncId, InventoryType type, Component title);

  PacketOperation setInventorySlot(int syncId, int revision, int slot, ItemStack item);

  PacketOperation closeInventory(int syncId);

  static SurfBukkitNmsPlayerPackets get() {
    return SurfBukkitNmsPacketBridges.get().getPlayerPackets();
  }
}
