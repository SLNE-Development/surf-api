package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.villager;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.villager.PacketAbstractVillager;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.PacketAgeableMobImpl;
import java.util.UUID;

public abstract class PacketAbstractVillagerImpl<Impl extends PacketAbstractVillager<Impl>> extends
    PacketAgeableMobImpl<Impl> implements PacketAbstractVillager<Impl> {

  public PacketAbstractVillagerImpl(UUID uuid, EntityType type) {
    super(uuid, type);
  }

  @Override
  public int shakeHeadTicks() {
    return get(SHAKE_HEAD_TICKS_INDEX, 0);
  }

  @Override
  public void shakeHeadTicks(int shakeHeadTicks) {
    set(SHAKE_HEAD_TICKS_INDEX, shakeHeadTicks);
    afterSet();
  }
}
