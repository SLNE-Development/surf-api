package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.golem;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.Direction;
import dev.slne.surf.surfapi.core.api.packet.entity.DyeColor;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.golem.PacketShulker;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PacketShulkerImpl extends PacketAbstractGolemImpl<PacketShulker> implements
    PacketShulker {

  public PacketShulkerImpl(UUID uuid) {
    super(uuid, EntityTypes.SHULKER);
  }

  @Override
  public Direction attachedFace() {
    return Direction.values()[get(ATTACHED_FACE_INDEX, Direction.DOWN.ordinal())];
  }

  @Override
  public void attachedFace(@NotNull Direction face) {
    set(ATTACHED_FACE_INDEX, face.ordinal());
    afterSet();
  }

  @Override
  public float peek() {
    return get(PEEK_INDEX, 0.0F);
  }

  @Override
  public void peek(float value) {
    set(PEEK_INDEX, value);
    afterSet();
  }

  @Override
  public Optional<DyeColor> color() {
    return Optional.ofNullable(DyeColor.getByWoolData(get(COLOR_INDEX, (byte) 16)));
  }

  @Override
  public void color(@Nullable DyeColor color) {
    set(COLOR_INDEX, color == null ? (byte) 16 : color.getWoolData());
    afterSet();
  }
}
