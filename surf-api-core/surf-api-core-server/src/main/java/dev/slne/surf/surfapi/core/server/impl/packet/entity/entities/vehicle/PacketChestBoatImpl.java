package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.vehicle;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.vehicle.PacketChestBoat;
import java.util.UUID;

public final class PacketChestBoatImpl extends PacketBoatImpl<PacketChestBoat> implements
    PacketChestBoat {

  public PacketChestBoatImpl(UUID uuid) {
    super(uuid, EntityTypes.CHEST_BOAT);
  }
}
