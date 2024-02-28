package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.villager;

import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.PacketAgeableMob;

public interface PacketAbstractVillager<Impl extends PacketAbstractVillager<Impl>> extends
    PacketAgeableMob<Impl> {

  int SHAKE_HEAD_TICKS_INDEX = 17;

  /**
   * If bigger than 0, causes this villager to shake his head.
   *
   * @return shake head ticks
   */
  int shakeHeadTicks();

  /**
   * If bigger than 0, causes this villager to shake his head.
   *
   * @param shakeHeadTicks shake head ticks
   */
  void shakeHeadTicks(int shakeHeadTicks);
}
