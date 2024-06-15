package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster;

import static com.google.common.base.Preconditions.checkNotNull;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

@CanBeSpawned
public interface PacketWarden extends PacketMonster<PacketWarden>, Spawnable {

  int ANGER_INDEX = 16;

  /**
   * Gets the anger level of this warden.
   * <p>
   * Anger is an integer from 0 to 150. Once a Warden reaches 80 anger at a target it will actively
   * pursue it.
   *
   * @return anger level
   */
  @Range(from = 0, to = 150)
  int anger();

  /**
   * Sets the anger level of this warden.
   * <p>
   * Anger is an integer from 0 to 150. Once a Warden reaches 80 anger at a target it will actively
   * pursue it.
   *
   * @param anger anger level
   */
  void anger(@Range(from = 0, to = 150) int anger);

  /**
   * Get the level of anger of this warden.
   *
   * @return The level of anger
   */
  @NotNull
  default AngerLevel angerLevel() {
    return AngerLevel.from(anger());
  }

  /**
   * Set the level of anger of this warden.
   *
   * @param level The level of anger
   */
  default void angerLevel(@NotNull AngerLevel level) {
    anger(checkNotNull(level, "level").ordinal() * 40);
  }

  enum AngerLevel {
    /**
     * Anger level 0-39.
     */
    CALM,
    /**
     * Anger level 40-79.
     */
    AGITATED,
    /**
     * Anger level 80 or above.
     */
    ANGRY;

    @Contract(pure = true)
    static AngerLevel from(int anger) {
      return anger < 40 ? CALM : anger < 80 ? AGITATED : ANGRY;
    }
  }
}
