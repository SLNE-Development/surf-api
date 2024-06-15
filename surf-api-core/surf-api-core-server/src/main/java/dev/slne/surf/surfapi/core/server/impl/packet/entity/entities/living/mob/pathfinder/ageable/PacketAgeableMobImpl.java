package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.PacketAgeableMob;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.PacketPathfinderMobImpl;
import java.util.UUID;

public abstract class PacketAgeableMobImpl<Impl extends PacketAgeableMob<Impl>> extends
    PacketPathfinderMobImpl<Impl> implements PacketAgeableMob<Impl> {

  public PacketAgeableMobImpl(UUID uuid, EntityType type) {
    super(uuid, type);
  }

  @Override
  public boolean baby() {
    return get(IS_BABY_INDEX, false);
  }

  @Override
  public void baby(boolean baby) {
    set(IS_BABY_INDEX, baby);
    afterSet();
  }
}
