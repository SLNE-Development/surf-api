package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.horse;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketCamel extends PacketAbstractHorse<PacketCamel>, Spawnable {

  int DASHING_INDEX = 18, LAST_POSE_CHANGE_TIME_INDEX = 19;

  boolean dashing();

  void dashing(boolean dashing);

  int lastPoseChangeTime();

  void lastPoseChangeTime(int lastPoseChangeTime);
}
