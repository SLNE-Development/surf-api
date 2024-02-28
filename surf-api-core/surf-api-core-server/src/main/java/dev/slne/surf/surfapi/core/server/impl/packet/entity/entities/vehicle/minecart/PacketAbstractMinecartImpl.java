package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.vehicle.minecart;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.vehicle.minecart.PacketAbstractMinecart;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityImpl;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public abstract class PacketAbstractMinecartImpl<Impl extends PacketAbstractMinecart<Impl>> extends
    PacketEntityImpl<Impl> implements PacketAbstractMinecart<Impl> {

  protected static final WrappedBlockState AIR = WrappedBlockState.getByGlobalId(0);

  private final int defaultBlockOffset;
  private final WrappedBlockState defaultBlockState;

  public PacketAbstractMinecartImpl(UUID uuid, EntityType type, int defaultBlockOffset,
      WrappedBlockState defaultBlockState) {
    super(uuid, type);
    this.defaultBlockOffset = defaultBlockOffset;
    this.defaultBlockState = defaultBlockState;
  }

  @Override
  public int hurtTime() {
    return get(HURT_TIME_INDEX, 0);
  }

  @Override
  public void hurtTime(int hurtTime) {
    set(HURT_TIME_INDEX, hurtTime);
    afterSet();
  }

  @Override
  public int hurtDirection() {
    return get(HURT_DIRECTION_INDEX, 1);
  }

  @Override
  public void hurtDirection(int hurtDirection) {
    set(HURT_DIRECTION_INDEX, hurtDirection);
    afterSet();
  }

  @Override
  public void damage(float damage) {
    set(DAMAGE_INDEX, damage);
    afterSet();
  }

  @Override
  public float damage() {
    return get(DAMAGE_INDEX, 0.0f);
  }

  @Override
  public void displayBlockData(@Nullable WrappedBlockState blockState) {
    if (blockState == null) {
      set(DISPLAY_BLOCK_DATA_INDEX, defaultBlockState.getGlobalId()); // Set to default block
      set(DISPLAY_BLOCK_INDEX, false); // No display block
    } else {
      set(DISPLAY_BLOCK_DATA_INDEX, blockState.getGlobalId());
      set(DISPLAY_BLOCK_INDEX, true); // Display block
    }

    afterSet();
  }

  @Override
  public WrappedBlockState displayBlockData() {
    final Integer globalID = get(DISPLAY_BLOCK_DATA_INDEX, null);
    return globalID == null ? defaultBlockState.clone() : WrappedBlockState.getByGlobalId(globalID);
  }

  @Override
  public void displayBlockOffset(int offset) {
    set(DISPLAY_BLOCK_OFFSET_INDEX, offset);
    set(DISPLAY_BLOCK_INDEX, true); // Display block
    afterSet();
  }

  @Override
  public int displayBlockOffset() {
    return get(DISPLAY_BLOCK_OFFSET_INDEX, defaultBlockOffset);
  }
}
