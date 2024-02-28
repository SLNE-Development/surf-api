package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketVex extends PacketMonster<PacketVex>, Spawnable {

  int VEX_FLAGS_ID = 16;

  byte CHARGING_FLAG = 0x01;

  /**
   * Gets the charging state of this entity.
   * <p>
   * When this entity is charging, it will have a glowing red texture.
   *
   * @return charging state
   */
  boolean charging();

  /**
   * Sets the charging state of this entity.
   * <p>
   * When this entity is charging, it will have a glowing red texture.
   *
   * @param charging new state
   */
  void charging(boolean charging);
}
