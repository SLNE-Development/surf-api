package dev.slne.surf.surfapi.core.api.util.pos;

import javax.annotation.concurrent.Immutable;
import org.jetbrains.annotations.Range;

@Immutable
public interface Hitbox {

  static Hitbox of(@Range(from = 0, to = ((long) Double.MAX_VALUE)) double width,
      @Range(from = 0, to = ((long) Double.MAX_VALUE)) double height) {
    return new HitboxImpl(width, height);
  }

  static Hitbox zero() {
    return HitboxImpl.ZERO;
  }

  @Range(from = 0, to = ((long) Double.MAX_VALUE))
  double width();

  @Range(from = 0, to = ((long) Double.MAX_VALUE))
  double height();
}
