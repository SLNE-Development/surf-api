package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketGuardian<Impl extends PacketGuardian<Impl>> extends PacketMonster<Impl>,
    Spawnable {

  int MOVING_INDEX = 16, TARGET_ID_INDEX = 17;

  /**
   * Check whether or not this guardian is moving.
   * <p>
   * While moving, the guardian's spikes are retracted and will not inflict thorns damage upon
   * entities that attack it. Additionally, a moving guardian cannot attack another entity. If
   * stationary (i.e. this method returns {@code false}), thorns damage is guaranteed and the
   * guardian may initiate laser attacks.
   *
   * @return true if moving, false if stationary
   */
  boolean moving();

  /**
   * Set whether or not this guardian is moving.
   * <p>
   * While moving, the guardian's spikes are retracted and will not inflict thorns damage upon
   * entities that attack it. Additionally, a moving guardian cannot attack another entity. If
   * stationary (i.e. this method returns {@code false}), thorns damage is guaranteed and the
   * guardian may initiate laser attacks.
   *
   * @param moving true if moving, false if stationary
   */
  void moving(boolean moving);

  /**
   * Get the target entity ID of this guardian or {@code 0} if no target.
   *
   * @return the target entity ID
   */
  int targetId();

  /**
   * Set the target entity ID of this guardian or {@code 0} if no target.
   *
   * @param targetId the target entity ID
   */
  void targetId(int targetId);
}
