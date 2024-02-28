package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketPolarBear;
import java.util.UUID;

public final class PacketPolarBearImpl extends PacketAnimalImpl<PacketPolarBear> implements
    PacketPolarBear {

  public PacketPolarBearImpl(UUID uuid) {
    super(uuid, EntityTypes.POLAR_BEAR);
  }

  @Override
  public boolean standing() {
    return get(STANDING_INDEX, false);
  }

  @Override
  public void standing(boolean standing) {
    set(STANDING_INDEX, standing);
    afterSet();
  }
}
