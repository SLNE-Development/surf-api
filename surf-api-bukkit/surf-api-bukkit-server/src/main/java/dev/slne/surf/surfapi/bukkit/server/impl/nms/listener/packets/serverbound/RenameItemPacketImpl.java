package dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.serverbound;

import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.serverbound.RenameItemPacket;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;

public final class RenameItemPacketImpl extends NmsServerboundPacketImpl<ServerboundRenameItemPacket> implements
    RenameItemPacket {

  public RenameItemPacketImpl(ServerboundRenameItemPacket nmsPacket) {
    super(nmsPacket);
  }

  @Override
  public String getNewName() {
    return getNmsPacket().getName();
  }
}
