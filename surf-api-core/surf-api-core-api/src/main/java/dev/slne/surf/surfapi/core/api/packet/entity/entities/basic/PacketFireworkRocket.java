package dev.slne.surf.surfapi.core.api.packet.entity.entities.basic;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.PacketEntity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Shootable;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketFireworkRocket extends PacketEntity<PacketFireworkRocket>, Shootable,
    Spawnable {

  int FIREWORK_ITEM_INDEX = 8, SHOOTER_ENTITY_ID_INDEX = 9, SHOT_AT_ANGLE_INDEX = 10;

  /**
   * Firework info
   *
   * @return the firework info
   */
  ItemStack fireworkItem();

  /**
   * Sets the Firework info
   *
   * @param fireworkItem the firework info
   */
  void fireworkItem(@NotNull ItemStack fireworkItem);

  /**
   * Entity ID of the entity wich used the firework (for elytra boosting) or {@code -1} if not
   * present
   *
   * @return the Entity ID or {@code -1} if not present
   */
  @Override
  int shooterEntityId();

  /**
   * Sets the Entity ID of the entity wich used the firework (for elytra boosting) or {@code -1} if
   * not present
   *
   * @param shooterEntityId the Entity ID or {@code -1} if not present
   */
  @Override
  void shooterEntityId(int shooterEntityId);

  /**
   * Gets weather the firework has a shooter or not
   *
   * @return {@code true} if the firework has a shooter, {@code false} otherwise
   */
  default boolean hasShooter() {
    return shooterEntityId() != -1;
  }

  /**
   * Is shot at angle (from a crossbow)
   *
   * @return if the firework is shot at angle
   */
  boolean shotAtAngle();

  /**
   * Sets if the firework is shot at angle (from a crossbow)
   *
   * @param shotAtAngle if the firework is shot at angle
   */
  void shotAtAngle(boolean shotAtAngle);
}
