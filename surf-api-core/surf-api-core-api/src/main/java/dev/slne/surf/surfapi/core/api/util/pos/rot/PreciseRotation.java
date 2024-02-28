package dev.slne.surf.surfapi.core.api.util.pos.rot;

import com.github.retrooper.packetevents.util.Vector3f;
import javax.annotation.concurrent.Immutable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@Immutable
public interface PreciseRotation {

  @Contract("_, _, _ -> new")
  static @NotNull PreciseRotation of(float x, float y, float z) {
    return new PreciseRotationImpl(x, y, z);
  }

  @Contract("_ -> new")
  static @NotNull PreciseRotation fromPacketEvents(@NotNull Vector3f packetEvents) {
    return of(packetEvents.x, packetEvents.y, packetEvents.z);
  }

  @Contract(pure = true)
  static PreciseRotation zero() {
    return PreciseRotationImpl.ZERO;
  }

  float x();

  float y();

  float z();

  @Contract("_, _, _ -> new")
  default PreciseRotation add(float x, float y, float z) {
    return of(x() + x, y() + y, z() + z);
  }

  @Contract("_, _, _ -> new")
  default PreciseRotation subtract(float x, float y, float z) {
    return add(-x, -y, -z);
  }

  Vector3f toPacketEvents();
}
