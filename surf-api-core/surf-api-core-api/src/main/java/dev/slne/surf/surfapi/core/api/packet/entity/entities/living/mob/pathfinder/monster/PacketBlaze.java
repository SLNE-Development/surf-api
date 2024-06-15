package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.ApiStatus;

@CanBeSpawned
public interface PacketBlaze extends PacketMonster<PacketBlaze>, Spawnable {

  int BLAZE_FLAGS_INDEX = 16;

  byte IS_ON_FIRE_FLAG = 0x01;

  /**
   * Gets whether this blaze is on fire.
   *
   * @return {@code true} if this blaze is on fire, {@code false} otherwise
   * @apiNote In favor of {@link #charged()}
   */
  @ApiStatus.Obsolete
  @Override
  default boolean onFire() {
    return charged();
  }

  /**
   * Sets whether this blaze is on fire.
   *
   * @param onFire {@code true} if this blaze should be on fire, {@code false} otherwise
   * @apiNote In favor of {@link #charged(boolean)}
   */
  @ApiStatus.Obsolete
  @Override
  default void onFire(boolean onFire) {
    charged(onFire);
  }

  /**
   * Gets whether this blaze is charged.
   *
   * @return {@code true} if this blaze is charged, {@code false} otherwise
   */
  boolean charged();

  /**
   * Sets whether this blaze is charged.
   *
   * @param charged {@code true} if this blaze should be charged, {@code false} otherwise
   */
  void charged(boolean charged);
}
