package dev.slne.surf.surfapi.core.api.util.pos;

import org.jetbrains.annotations.Range;

import javax.annotation.concurrent.Immutable;

@Immutable
public interface Hitbox {

    @Range(from = 0, to = ((long) Double.MAX_VALUE))
    double width();

    @Range(from = 0, to = ((long) Double.MAX_VALUE))
    double height();

    static Hitbox of(@Range(from = 0, to = ((long) Double.MAX_VALUE)) double width,
                     @Range(from = 0, to = ((long) Double.MAX_VALUE)) double height) {
        return new HitboxImpl(width, height);
    }

    static Hitbox zero() {
        return HitboxImpl.ZERO;
    }
}
