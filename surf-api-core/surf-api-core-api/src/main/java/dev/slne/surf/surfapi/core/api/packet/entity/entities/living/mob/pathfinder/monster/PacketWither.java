package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import dev.slne.surf.surfapi.core.api.util.ById;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketWither extends PacketMonster<PacketWither>, Spawnable {

  int INVULNERABLE_TIME_INDEX = 19;

  /**
   * Returns the ID of the entity targeted by the given head.
   *
   * @param head the head to get the target of
   * @return the ID of the entity targeted by the given head, or 0 if none is targeted
   */
  int targetEntityId(@NotNull Head head);

  /**
   * Sets the ID of the entity targeted by the given head.
   *
   * @param head     the head to set the target of
   * @param entityId the ID of the entity to target or 0 to remove the target
   */
  void targetEntityId(@NotNull Head head, int entityId);

  /**
   * @return ticks the wither is invulnerable for
   */
  int invulnerableTicks();

  /**
   * Sets for how long in the future, the wither should be invulnerable.
   *
   * @param ticks ticks the wither is invulnerable for
   */
  void invulnerableTicks(int ticks);

  /**
   * Represents one of the Wither's heads.
   */
  enum Head implements ById {
    CENTER(16),
    LEFT(17),
    RIGHT(18);

    private final int index;

    @Contract(pure = true)
    Head(int index) {
      this.index = index;
    }

    @Contract(pure = true)
    @Override
    public int id() {
      return index;
    }
  }
}
