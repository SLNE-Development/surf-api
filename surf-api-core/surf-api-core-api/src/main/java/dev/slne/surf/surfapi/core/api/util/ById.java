package dev.slne.surf.surfapi.core.api.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ById {

  static <T extends Enum<T> & ById> @NotNull Int2ObjectMap<T> build(@NotNull Class<T> clazz) {
    return Util.byIdMap(ById::id, clazz.getEnumConstants());
  }

  int id();

  @FunctionalInterface
  interface ByByteId {

    byte id();
  }
}
