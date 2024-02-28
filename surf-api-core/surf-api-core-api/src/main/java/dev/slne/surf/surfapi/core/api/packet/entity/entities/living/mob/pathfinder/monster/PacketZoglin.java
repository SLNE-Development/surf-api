package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketZoglin extends PacketMonster<PacketZoglin>, Spawnable {

  int BABY_INDEX = 16;

  /**
   * Gets whether the zoglin is a baby
   *
   * @return Whether the zoglin is a baby
   */
  boolean baby();

  /**
   * Sets whether the zoglin is a baby
   *
   * @param baby Whether the zoglin is a baby
   */
  void baby(boolean baby);
}
