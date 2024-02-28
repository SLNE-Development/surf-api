package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.tameable;

import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketAnimal;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public interface PacketTameableAnimal<Impl extends PacketTameableAnimal<Impl>> extends
    PacketAnimal<Impl> {

  int TAMEABLE_FLAGS = 17, OWNER_UUID = 18;

  byte SITTING_FLAG = 0x00, TAMED_FLAG = 0x04;

  /**
   * Check if this is tamed
   *
   * @return true if this has been tamed
   */
  boolean tamed();

  /**
   * Sets if this has been tamed. Not necessary if the method {@link #ownerUuid(UUID)} has been
   * used, as it tames automatically.
   *
   * @param tame true if tame
   */
  void setTamed(boolean tame);

  /**
   * Gets the owners UUID
   *
   * @return the owners UUID, or empty if not owned
   */
  Optional<UUID> ownerUuid();

  /**
   * Sets the owners UUID
   *
   * @param ownerUuid the owners UUID, or null to remove
   */
  void ownerUuid(@Nullable UUID ownerUuid);

  /**
   * Check if this is sitting
   *
   * @return true if this is sitting
   */
  boolean sitting();

  /**
   * Sets if this is sitting
   *
   * @param sitting true if sitting
   */
  void setSitting(boolean sitting);
}
