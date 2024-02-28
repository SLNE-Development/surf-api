package dev.slne.surf.surfapi.core.api.util;

import com.github.retrooper.packetevents.protocol.world.Location;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.math.GenericMath;

public interface LocationFactory {

  static double distanceSquared(@NotNull Location locationA, @NotNull Location locationB) {
    return square(locationA.getX() - locationB.getX()) + square(locationA.getY() - locationB.getY())
        + square(locationA.getZ() - locationB.getZ());
  }

  static double distance(@NotNull Location locationA, @NotNull Location locationB) {
    return GenericMath.sqrt(distanceSquared(locationA, locationB));
  }

  @ApiStatus.Internal
  @Contract(pure = true)
  static double square(double value) {
    return value * value;
  }
}
