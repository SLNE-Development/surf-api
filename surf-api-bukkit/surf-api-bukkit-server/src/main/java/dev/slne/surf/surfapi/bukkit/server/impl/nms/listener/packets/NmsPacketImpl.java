package dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets;

import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.NmsPacket;
import java.util.function.Supplier;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;

@Getter
public abstract class NmsPacketImpl<Nms extends net.minecraft.network.protocol.Packet<Listener>, Listener extends PacketListener> implements
    NmsPacket {

  private final Class<Nms> nmsClass;
  private final Nms nmsPacket;
  private final Class<?> packetClass;

  protected NmsPacketImpl(Nms nmsPacket) {
    this.nmsClass = (Class<Nms>) nmsPacket.getClass();
    this.nmsPacket = nmsPacket;

    for (final Class<?> implementingInterface : getClass().getInterfaces()) {
      if (NmsPacket.class.isAssignableFrom(implementingInterface)) {
        this.packetClass = implementingInterface;
        return;
      }
    }

    throw new IllegalStateException("No packet interface found for " + getClass().getName());
  }

  @Override
  public Class<?> getPacketClass() {
    return packetClass;
  }

  public static NmsPacketImpl<?, ?> getFromApi(NmsPacket nmsPacket) {
    if (!(nmsPacket instanceof NmsPacketImpl<?,?> nmsPacketImpl)) {
      throw new IllegalArgumentException("Invalid NmsPacket implementation: " + nmsPacket.getClass().getName());
    }

    return nmsPacketImpl;
  }
}
