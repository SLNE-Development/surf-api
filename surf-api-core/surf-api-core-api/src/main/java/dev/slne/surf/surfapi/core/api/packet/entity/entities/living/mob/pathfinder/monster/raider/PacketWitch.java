package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.raider;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketWitch extends PacketRaider<PacketWitch>, Spawnable {

  int DRINKING_POTION_INDEX = 17;

  /**
   * Gets whether the witch is drinking a potion
   *
   * @return whether the witch is drinking a potion
   */
  boolean drinkingPotion();

  /**
   * Sets whether the witch is drinking a potion
   *
   * @param drinkingPotion whether the witch is drinking a potion
   */
  void drinkingPotion(boolean drinkingPotion);
}
