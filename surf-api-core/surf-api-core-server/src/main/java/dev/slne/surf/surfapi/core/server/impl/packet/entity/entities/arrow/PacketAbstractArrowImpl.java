package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.arrow;

import static com.google.common.base.Preconditions.checkArgument;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.arrow.PacketAbstractArrow;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityImpl;
import java.util.UUID;
import org.jetbrains.annotations.Range;

public abstract class PacketAbstractArrowImpl<Impl extends PacketAbstractArrow<Impl>> extends
    PacketEntityImpl<Impl> implements PacketAbstractArrow<Impl> {

  public PacketAbstractArrowImpl(UUID uuid, EntityType type) {
    super(uuid, type);
  }

  @Override
  public boolean critical() {
    return getMaskBit(ABSTRACT_ARROW_BIT_MASK_INDEX, IS_CRITICAL_BIT);
  }

  @Override
  public void critical(boolean critical) {
    setMaskBit(ABSTRACT_ARROW_BIT_MASK_INDEX, IS_CRITICAL_BIT, critical);
    afterSet();
  }

  @Override
  public boolean noClip() {
    return getMaskBit(ABSTRACT_ARROW_BIT_MASK_INDEX, IS_NO_CLIP_BIT);
  }

  @Override
  public void noClip(boolean noClip) {
    setMaskBit(ABSTRACT_ARROW_BIT_MASK_INDEX, IS_NO_CLIP_BIT, noClip);
    afterSet();
  }

  @Override
  public @Range(from = 0, to = Byte.MAX_VALUE) int piercingLevel() {
    return get(PIERCING_LEVEL_INDEX, (byte) 0);
  }

  @Override
  public void piercingLevel(int piercingLevel) {
    checkArgument(0 <= piercingLevel && piercingLevel <= Byte.MAX_VALUE,
        "Piercing level (%s) out of range, expected 0 < level < 127", piercingLevel);

    setByte(PIERCING_LEVEL_INDEX, (byte) piercingLevel);
    afterSet();
  }
}
