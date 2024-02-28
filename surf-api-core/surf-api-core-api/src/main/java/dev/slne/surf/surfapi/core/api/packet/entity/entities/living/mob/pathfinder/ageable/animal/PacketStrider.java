package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketStrider extends PacketAnimal<PacketStrider>, Spawnable {

  int BOOST_TICKS_INDEX = 17, SHIVERING_INDEX = 18, SADDLED_INDEX = 19;

  /**
   * Total time to "boost" with warped fungus on a stick for
   *
   * @return the boostTicks
   */
  int boostTicks();

  /**
   * Sets the total time to "boost" with warped fungus on a stick for
   *
   * @param boostTicks the boostTicks to set
   */
  void boostTicks(int boostTicks);

  /**
   * Check whether this strider is out of warm blocks and shivering.
   *
   * @return true if shivering, false otherwise
   */
  boolean shivering();

  /**
   * Set whether this strider is shivering.
   *
   * @param shivering its new shivering state
   */
  void shivering(boolean shivering);

  /**
   * Check whether this strider has a saddle.
   *
   * @return true if saddled, false otherwise
   */
  boolean hasSaddle();

  /**
   * Set whether this strider is saddled.
   *
   * @param hasSaddle its new saddled state
   */
  void hasSaddle(boolean hasSaddle);
}
