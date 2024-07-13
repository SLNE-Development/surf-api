package dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets.player;

import static com.google.common.base.Preconditions.checkNotNull;

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation;
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.player.SurfBukkitNmsPlayerPackets;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets.PacketOperationImpl;
import dev.slne.surf.surfapi.bukkit.server.nms.NmsUtil;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.math.BlockPosition;
import javax.annotation.ParametersAreNonnullByDefault;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

@ParametersAreNonnullByDefault
public final class SurfBukkitNmsPlayerPacketsImpl implements SurfBukkitNmsPlayerPackets, NmsUtil {

  @Override
  public PacketOperation openSignEditor(BlockPosition position, boolean frontSide) {
    checkNotNull(position, "position");

    return PacketOperationImpl.simple(
        player -> new ClientboundOpenSignEditorPacket(toNms(position), frontSide));
  }

  @Override
  public PacketOperation openInventory(int syncId, InventoryType type, Component title) {
    checkNotNull(type, "type");
    checkNotNull(title, "title");
    return PacketOperationImpl.simple(player -> new ClientboundOpenScreenPacket(syncId, toNms(type),
        PaperAdventure.asVanilla(title)));
  }

  @Override
  public PacketOperation setInventorySlot(int syncId, int revision, int slot, ItemStack item) {
    checkNotNull(item, "item");

    return PacketOperationImpl.simple(
        player -> new ClientboundContainerSetSlotPacket(syncId, revision, slot, toNms(item)));
  }

  @Override
  public PacketOperation closeInventory(int syncId) {
    return PacketOperationImpl.simple(player -> new ClientboundContainerClosePacket(syncId));
  }
}
