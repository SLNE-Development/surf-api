package dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.serverbound;

import dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.NmsPacketImpl;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;


public abstract class NmsServerboundPacketImpl<Nms extends Packet<ServerGamePacketListener>> extends
    NmsPacketImpl<Nms, ServerGamePacketListener> {

  protected NmsServerboundPacketImpl(Nms nmsPacket) {
    super(nmsPacket);
  }
}
