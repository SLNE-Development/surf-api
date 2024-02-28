package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketSpider extends PacketMonster<PacketSpider>, Spawnable {

  int SPIDER_FLAGS_INDEX = 16;

  byte CLIMBING_FLAG = 0x01;

  /**
   * Gets if the entity is climbing.
   *
   * @return if the entity is climbing
   */
  boolean climbing();

  /**
   * Sets if the entity is climbing.
   *
   * @param climbing if the entity is climbing
   */
  void climbing(boolean climbing);
}
