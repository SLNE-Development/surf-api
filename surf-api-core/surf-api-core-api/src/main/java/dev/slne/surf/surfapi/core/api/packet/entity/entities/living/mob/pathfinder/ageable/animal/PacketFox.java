package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CanBeSpawned
public interface PacketFox extends PacketAnimal<PacketFox>, Spawnable {

  /**
   * Indexes of the fox data.
   */
  int TYPE_INDEX = 17, FOX_BIT_MASK_INDEX = 18, FIRST_TRUSTED_PLAYER_INDEX = 19, SECOND_TRUSTED_PLAYER_INDEX = 20;

  /**
   * Bit mask flags for the fox data.
   */
  byte SITTING_FLAG = 0x01, CROUCHING_FLAG = 0x04, INTERESTED_FLAG = 0x08, LEAPING_FLAG = 0x10, SLEEPING_FLAG = 0x20,
      FACEPLANTED_FLAG = 0x40, DEFENDING_FLAG = (byte) 0x80;

  /**
   * Gets the current type of this fox.
   *
   * @return Type of the fox.
   */
  Type foxType();

  /**
   * Sets the current type of this fox.
   *
   * @param type New type of this fox.
   */
  void foxType(@NotNull Type type);

  /**
   * Checks if this fox is sitting.
   *
   * @return true if sitting
   */
  boolean sitting();

  /**
   * Sets if this fox is sitting.
   *
   * @param sitting true if sitting
   */
  void sitting(boolean sitting);

  /**
   * Checks if this fox is crouching.
   *
   * @return true if crouching
   */
  boolean crouching();

  /**
   * Sets if this fox is crouching.
   *
   * @param crouching true if crouching
   */
  void crouching(boolean crouching);

  /**
   * Gets if this fox is interested.
   *
   * @return true if interested
   */
  boolean interested();

  /**
   * Sets if this fox is interested.
   *
   * @param interested true if interested
   */
  void interested(boolean interested);

  /**
   * Gets if this fox is leaping.
   *
   * @return true if leaping
   */
  boolean leaping();

  /**
   * Sets if this fox is leaping.
   *
   * @param leaping true if leaping
   */
  void leaping(boolean leaping);

  /**
   * Gets if this fox is sleeping.
   *
   * @return true if sleeping
   */
  boolean sleeping();

  /**
   * Sets if this fox is sleeping.
   *
   * @param sleeping true if sleeping
   */
  void sleeping(boolean sleeping);

  /**
   * Gets if this fox is faceplanted.
   *
   * @return true if faceplanted
   */
  boolean faceplanted();

  /**
   * Sets if this fox is faceplanted.
   *
   * @param faceplanted true if faceplanted
   */
  void faceplanted(boolean faceplanted);

  /**
   * Gets if this fox is defending.
   *
   * @return true if defending
   */
  boolean defending();

  /**
   * Sets if this fox is defending.
   *
   * @param defending true if defending
   */
  void defending(boolean defending);

  /**
   * Gets the UUID of the first trusted player.
   *
   * @return The UUID of the first trusted player.
   */
  Optional<UUID> firstTrustedPlayer();

  /**
   * Sets the UUID of the first trusted player or {@code null} to remove the trusted player (the
   * second trusted player must be removed first).
   *
   * @param uuid The UUID of the first trusted player.
   * @throws IllegalStateException If the second trusted player is not removed first.
   */
  void firstTrustedPlayer(@Nullable UUID uuid);

  /**
   * Gets the UUID of the second trusted player.
   *
   * @return The UUID of the second trusted player.
   */
  Optional<UUID> secondTrustedPlayer();

  /**
   * Sets the UUID of the second trusted player or {@code null} to remove the trusted player.
   *
   * @param uuid The UUID of the second trusted player.
   */
  void secondTrustedPlayer(@Nullable UUID uuid);

  /**
   * Represents the various different fox types there are.
   */
  enum Type {
    RED,
    SNOW
  }
}
