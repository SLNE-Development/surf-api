package dev.slne.surf.surfapi.core.api.packet.entity.entities.arrow;


import dev.slne.surf.surfapi.core.api.packet.entity.entities.PacketEntity;
import org.jetbrains.annotations.Range;

public interface PacketAbstractArrow<Impl extends PacketAbstractArrow<Impl>> extends
    PacketEntity<Impl> {

  int ABSTRACT_ARROW_BIT_MASK_INDEX = 8, PIERCING_LEVEL_INDEX = 9;

  byte IS_CRITICAL_BIT = 0x01, IS_NO_CLIP_BIT = 0x02;

  boolean critical();

  void critical(boolean critical);

  boolean noClip();

  void noClip(boolean noClip);

  @Range(from = 0, to = Byte.MAX_VALUE)
  int piercingLevel();

  void piercingLevel(@Range(from = 0, to = Byte.MAX_VALUE) int piercingLevel);
}
