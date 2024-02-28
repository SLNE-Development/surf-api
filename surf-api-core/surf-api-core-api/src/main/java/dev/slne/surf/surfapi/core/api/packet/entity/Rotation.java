package dev.slne.surf.surfapi.core.api.packet.entity;

import org.jetbrains.annotations.NotNull;

public enum Rotation {
  NONE,
  CLOCKWISE_45,
  CLOCKWISE,
  CLOCKWISE_135,
  FLIPPED,
  FLIPPED_45,
  COUNTER_CLOCKWISE,
  COUNTER_CLOCKWISE_45;

  private static final Rotation[] rotations = values();

  public @NotNull Rotation rotateClockwise() {
    return rotations[this.ordinal() + 1 & 7];
  }

  public @NotNull Rotation rotateCounterClockwise() {
    return rotations[this.ordinal() - 1 & 7];
  }
}
