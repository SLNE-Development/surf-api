package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.wateranimal.fish;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.wateranimal.fish.PacketCod;
import java.util.UUID;

public final class PacketCodImpl extends PacketAbstractFishImpl<PacketCod> implements PacketCod {

  public PacketCodImpl(UUID uuid) {
    super(uuid, EntityTypes.COD);
  }
}
