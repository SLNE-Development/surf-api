package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.raider;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.raider.illager.PacketAbstractIllager;

@CanBeSpawned
public interface PacketPillager extends PacketAbstractIllager<PacketPillager>, Spawnable {

  int CHARGING_INDEX = 17;

  /**
   * Gets if the piglin is currently charging the item in their hand.
   *
   * @return is charging
   */
  boolean chargingCrossbow();

  /**
   * Causes the piglin to appear as if they are charging a crossbow.
   * <p>
   * This works with any item currently held in the piglin's hand.
   *
   * @param chargingCrossbow is charging
   */
  void chargingCrossbow(boolean chargingCrossbow);
}
