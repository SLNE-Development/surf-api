package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.basic;

import static com.google.common.base.Preconditions.checkNotNull;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import dev.slne.surf.surfapi.core.api.packet.entity.Rotation;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Orientation;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.frame.PacketItemFrame;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityImpl;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public sealed class PacketItemFrameImpl<Impl extends PacketItemFrame<Impl>> extends
    PacketEntityImpl<Impl> implements PacketItemFrame<Impl> permits PacketGlowItemFrameImpl {

  private Orientation orientation = Orientation.DOWN;

  public PacketItemFrameImpl(UUID uuid) {
    super(uuid, EntityTypes.ITEM_FRAME);
  }

  protected PacketItemFrameImpl(UUID uuid, EntityType type) {
    super(uuid, type);
  }

  @Override
  public ItemStack item() {
    return get(ITEM_INDEX, ItemStack.EMPTY.copy());
  }

  @Override
  public void item(@NotNull ItemStack item) {
    set(ITEM_INDEX, checkNotNull(item, "item"));
    afterSet();
  }

  @Override
  public Rotation rotation() {
    return Rotation.values()[get(ROTATION_INDEX, 0)];
  }

  @Override
  public void rotation(@NotNull Rotation rotation) {
    set(ROTATION_INDEX, rotation.ordinal());
    afterSet();
  }

  @Override
  public Orientation faceDirection() {
    return orientation;
  }

  @Override
  public void faceDirection(@NotNull Orientation faceDirection) {
    this.orientation = checkNotNull(faceDirection, "faceDirection");
  }

  @Override
  public int getData() {
    return orientation.ordinal();
  }
}
