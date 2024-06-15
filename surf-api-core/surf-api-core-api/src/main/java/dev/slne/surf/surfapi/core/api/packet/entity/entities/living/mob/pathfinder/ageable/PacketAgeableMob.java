package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable;

import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.PacketPathfinderMob;

public interface PacketAgeableMob<Impl extends PacketPathfinderMob<Impl>> extends
    PacketPathfinderMob<Impl> {

  int IS_BABY_INDEX = 16;

  boolean baby();

  void baby(boolean baby);
}
