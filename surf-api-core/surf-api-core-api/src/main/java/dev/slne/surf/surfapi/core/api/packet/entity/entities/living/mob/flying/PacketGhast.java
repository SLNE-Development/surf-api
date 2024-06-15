package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.flying;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketGhast extends PacketFlying<PacketGhast>, Spawnable {

  int CHARGING_INDEX = 16;

  /**
   * Gets whether the Ghast is charging
   *
   * @return Whether the Ghast is charging
   */
  boolean charging();

  /**
   * Sets whether the Ghast is charging
   *
   * @param charging Whether the Ghast is charging
   */
  void charging(boolean charging);
}
