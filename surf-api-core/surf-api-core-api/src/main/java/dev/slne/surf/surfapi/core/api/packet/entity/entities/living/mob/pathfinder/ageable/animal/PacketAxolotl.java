package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.Useless;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketAxolotl extends PacketAnimal<PacketAxolotl>, Spawnable {

  int VARIANT_INDEX = 17, PLAYING_DEAD_INDEX = 18, SPAWNED_FROM_BUCKET_INDEX = 19;

  /**
   * Get the variant of this axolotl.
   *
   * @return axolotl variant
   */
  Variant variant();

  /**
   * Set the variant of this axolotl.
   *
   * @param variant axolotl variant
   */
  void variant(@NotNull Variant variant);

  /**
   * Gets if this axolotl is playing dead.
   * <p>
   * An axolotl may play dead when it is damaged underwater.
   *
   * @return playing dead status
   */
  boolean playingDead();

  /**
   * Sets if this axolotl is playing dead.
   * <p>
   * An axolotl may play dead when it is damaged underwater.
   *
   * @param playingDead playing dead status
   */
  void playingDead(boolean playingDead);

  /**
   * Gets if this entity originated from a bucket.
   *
   * @return originated from bucket
   */
  @Useless
  // Irrelevant for packet entities
  boolean spawnedFromBucket();

  /**
   * Sets if this entity originated from a bucket.
   *
   * @param spawnedFromBucket originated from bucket
   */
  @Useless
  // Irrelevant for packet entities
  void spawnedFromBucket(boolean spawnedFromBucket);

  /**
   * Represents the variant of a axolotl - ie its color.
   */
  enum Variant {

    /**
     * Leucistic (pink) axolotl.
     */
    LUCY,
    /**
     * Brown axolotl.
     */
    WILD,
    /**
     * Gold axolotl.
     */
    GOLD,
    /**
     * Cyan axolotl.
     */
    CYAN,
    /**
     * Blue axolotl.
     */
    BLUE
  }
}
