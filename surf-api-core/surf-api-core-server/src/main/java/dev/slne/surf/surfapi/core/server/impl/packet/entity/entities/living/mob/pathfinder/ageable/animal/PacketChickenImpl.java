package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketChicken;
import java.util.UUID;

public final class PacketChickenImpl extends PacketAnimalImpl<PacketChicken> implements
    PacketChicken {

  public PacketChickenImpl(UUID uuid) {
    super(uuid, EntityTypes.CHICKEN);
  }
}
