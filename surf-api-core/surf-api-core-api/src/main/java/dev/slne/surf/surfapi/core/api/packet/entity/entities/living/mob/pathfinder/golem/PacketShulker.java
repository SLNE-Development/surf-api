package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.golem;

import com.github.retrooper.packetevents.protocol.world.Direction;
import dev.slne.surf.surfapi.core.api.packet.entity.DyeColor;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CanBeSpawned
public interface PacketShulker extends PacketAbstractGolem<PacketShulker>, Spawnable {

  int ATTACHED_FACE_INDEX = 16, PEEK_INDEX = 17, COLOR_INDEX = 18;

  /**
   * Gets the face to which the shulker is attached.
   *
   * @return the face to which the shulker is attached
   */
  Direction attachedFace();

  /**
   * Sets the face to which the shulker is attached.
   *
   * @param face the face to attach the shulker to
   */
  void attachedFace(@NotNull Direction face);

  /**
   * Gets the peek state of the shulker between 0.0 and 1.0.
   *
   * @return the peek state of the shulker between 0.0 and 1.0
   */
  float peek();

  /**
   * Sets the peek state of the shulker, should be in between 0.0 and 1.0.
   *
   * @param value peek state of the shulker, should be in between 0.0 and 1.0
   * @throws IllegalArgumentException thrown if the value exceeds the valid range in between of 0.0
   *                                  and 1.0
   */
  void peek(float value);

  /**
   * Gets the color of the shulker if it has one (else its default color).
   *
   * @return the color of the shulker
   */
  Optional<DyeColor> color();

  /**
   * Sets the color of the shulker.
   *
   * @param color the color of the shulker or null to reset to default
   */
  void color(@Nullable DyeColor color);
}
