package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.piglin;

import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.PacketMonster;

public interface PacketBasePiglin<Impl extends PacketBasePiglin<Impl>> extends PacketMonster<Impl> {

  /**
   * The index of the immune to zombification flag.
   */
  int IMMUNE_TO_ZOMBIFICATION_INDEX = 16;

  /**
   * Gets whether the piglin is immune to zombification.
   *
   * @return Whether the piglin is immune to zombification
   */
  boolean immuneToZombification();

  /**
   * Sets whether the piglin is immune to zombification.
   *
   * @param flag Whether the piglin is immune to zombification
   */
  void immuneToZombification(boolean flag);
}
