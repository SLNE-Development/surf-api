package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.golem;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketSnowGolem extends PacketAbstractGolem<PacketSnowGolem>, Spawnable {

  int SNOW_GOLEM_FLAG_INDEX = 16;

  byte SNOW_GOLEM_FLAG = 0x10;

  /**
   * Gets whether this snowman is in "derp mode", meaning it is not wearing a pumpkin.
   *
   * @return True if the snowman is bald, false if it is wearing a pumpkin
   */
  boolean derp();

  /**
   * Sets whether this snowman is in "derp mode", meaning it is not wearing a pumpkin. NOTE: This
   * value is not persisted to disk and will therefore reset when the chunk is reloaded.
   *
   * @param derpMode True to remove the pumpkin, false to add a pumpkin
   */
  void derp(boolean derpMode);
}
