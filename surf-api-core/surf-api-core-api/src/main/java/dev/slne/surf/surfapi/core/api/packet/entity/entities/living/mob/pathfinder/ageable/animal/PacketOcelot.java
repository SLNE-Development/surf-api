package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketOcelot extends PacketAnimal<PacketOcelot>, Spawnable {

  int TRUSTING_INDEX = 17;

  /**
   * Checks if this ocelot trusts players.
   *
   * @return true if it trusts players
   */
  boolean trusting();

  /**
   * Sets if this ocelot trusts players.
   *
   * @param trusting true if it trusts players
   */
  void trusting(boolean trusting);
}
