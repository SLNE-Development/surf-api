package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketBee extends PacketAnimal<PacketBee>, Spawnable {

  int BEE_FLAGS_INDEX = 17, ANGER_TIME_INDEX = 18;

  byte ANGRY_FLAG = 0x02, HAS_STUNG_FLAG = 0x04, HAS_NECTAR_FLAG = 0x08;

  /**
   * Get if the bee is angry.
   *
   * @return angry
   */
  boolean angry();

  /**
   * Set if the bee is angry.
   *
   * @param angry angry
   */
  void angry(boolean angry);

  /**
   * Get if the bee has stung.
   *
   * @return has stung
   */
  boolean hasStung();

  /**
   * Set if the bee has stung.
   *
   * @param hasStung has stung
   */
  void hasStung(boolean hasStung);

  /**
   * Get if the bee has nectar.
   *
   * @return nectar
   */
  boolean hasNectar();

  /**
   * Set if the bee has nectar.
   *
   * @param hasNectar whether the entity has nectar
   */
  void hasNectar(boolean hasNectar);

  /**
   * Get the amount of time the bee has been angry for in ticks.
   *
   * @return anger time
   */
  int angerTime();

  /**
   * Set the amount of time the bee has been angry for in ticks.
   *
   * @param angerTime anger time
   */
  void angerTime(int angerTime);
}
