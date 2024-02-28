package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.vehicle.minecart;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.vehicle.minecart.PacketMinecart;
import java.util.UUID;

public final class PacketMinecartImpl extends PacketAbstractMinecartImpl<PacketMinecart> implements
    PacketMinecart {

  public PacketMinecartImpl(UUID uuid) {
    super(uuid, EntityTypes.MINECART, 6, AIR);
  }
}
