package dev.slne.surf.api.minestom.server.impl.mixin.duck;

import net.minestom.server.coordinate.Point;
import org.jspecify.annotations.Nullable;

public interface EntityTrackerEntryDuck {

    @Nullable
    Point surf$lastPosition();
}
