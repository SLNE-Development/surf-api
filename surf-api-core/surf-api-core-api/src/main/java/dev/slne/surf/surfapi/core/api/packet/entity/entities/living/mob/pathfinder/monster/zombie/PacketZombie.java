package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.zombie;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.PacketMonster;

@CanBeSpawned
public interface PacketZombie<Impl extends PacketZombie<Impl>> extends PacketMonster<Impl>,
    Spawnable {

  int BABY_INDEX = 16, BECOMING_DROWNED_INDEX = 18;

  /**
   * Gets whether the zombie is a baby
   *
   * @return Whether the zombie is a baby
   */
  boolean baby();

  /**
   * Sets whether the zombie is a baby
   *
   * @param baby Whether the zombie is a baby
   */
  void baby(boolean baby);

  /**
   * Gets whether the zombie is a villager
   *
   * @return Whether the zombie is a villager
   */
  boolean becomingDrowned();

  /**
   * Sets whether the zombie is a villager
   *
   * @param becomingDrowned Whether the zombie is a villager
   */
  void becomingDrowned(boolean becomingDrowned);
}
