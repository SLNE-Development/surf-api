package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.Range;

@CanBeSpawned
public interface PacketSlime extends PacketMob<PacketSlime>, Spawnable {

  int SIZE_INDEX = 16;

  /**
   * Gets the size of the slime.
   *
   * @return The size of the slime
   */
  int size();

  /**
   * Sets the new size of the slime.
   *
   * @param size The new size of the slime.
   */
  void size(@Range(from = 0, to = Integer.MAX_VALUE) int size);
}
