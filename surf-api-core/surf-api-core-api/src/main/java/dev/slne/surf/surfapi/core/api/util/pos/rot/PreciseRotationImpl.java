package dev.slne.surf.surfapi.core.api.util.pos.rot;

import com.github.retrooper.packetevents.util.Vector3f;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

record PreciseRotationImpl(float x, float y, float z) implements PreciseRotation {

  static final PreciseRotation ZERO = new PreciseRotationImpl(0, 0, 0);

  @Contract(value = " -> new", pure = true)
  @Override
  public @NotNull Vector3f toPacketEvents() {
    return new Vector3f(x, y, z);
  }
}
