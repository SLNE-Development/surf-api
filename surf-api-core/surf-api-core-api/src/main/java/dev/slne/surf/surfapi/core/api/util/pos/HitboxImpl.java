package dev.slne.surf.surfapi.core.api.util.pos;

import static com.google.common.base.Preconditions.*;

public record HitboxImpl(double width, double height) implements Hitbox {

    public static HitboxImpl ZERO = new HitboxImpl(0, 0);

    public HitboxImpl {
        checkArgument(width > 0, "width must be positive");
        checkArgument(height > 0, "height must be positive");
    }
}
