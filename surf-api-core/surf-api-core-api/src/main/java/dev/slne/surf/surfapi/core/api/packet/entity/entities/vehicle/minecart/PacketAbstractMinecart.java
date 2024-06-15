package dev.slne.surf.surfapi.core.api.packet.entity.entities.vehicle.minecart;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.PacketEntity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.SpawnVelocity;
import org.jetbrains.annotations.Nullable;

public interface PacketAbstractMinecart<Impl extends PacketAbstractMinecart<Impl>> extends
    PacketEntity<Impl>, SpawnVelocity {

  int HURT_TIME_INDEX = 8, HURT_DIRECTION_INDEX = 9, DAMAGE_INDEX = 10, DISPLAY_BLOCK_DATA_INDEX = 11,
      DISPLAY_BLOCK_OFFSET_INDEX = 12, DISPLAY_BLOCK_INDEX = 13;

  /**
   * Gets the hurt time of the minecart.
   *
   * @return The hurt time
   */
  int hurtTime();

  /**
   * Sets the hurt time of the minecart.
   *
   * @param hurtTime The new hurt time
   */
  void hurtTime(int hurtTime);

  /**
   * Gets the hurt direction of the minecart.
   *
   * @return The hurt direction
   */
  int hurtDirection();

  /**
   * Sets the hurt direction of the minecart.
   *
   * @param hurtDirection The new hurt direction
   */
  void hurtDirection(int hurtDirection);

  /**
   * Sets a minecart's damage.
   *
   * @param damage over 40 to "kill" a minecart
   */
  void damage(float damage);

  /**
   * Gets a minecart's damage.
   *
   * @return The damage
   */
  float damage();

  /**
   * Sets the display block for this minecart. Passing a null value will set the minecart to have no
   * display block.
   *
   * @param blockState the material to set as display block.
   */
  void displayBlockData(@Nullable WrappedBlockState blockState);

  /**
   * Gets the display block for this minecart. This function will return air if the minecart has no
   * display block.
   *
   * @return the block displayed by this minecart.
   */
  WrappedBlockState displayBlockData();

  /**
   * Sets the offset of the display block.
   *
   * @param offset the block offset to set for this minecart.
   */
  void displayBlockOffset(int offset);

  /**
   * Gets the offset of the display block.
   *
   * @return the current block offset for this minecart.
   */
  int displayBlockOffset();
}
