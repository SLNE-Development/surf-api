package dev.slne.surf.surfapi.core.api.reflection;

import static com.google.common.base.Preconditions.checkNotNull;

import dev.slne.surf.surfapi.core.api.SurfCoreApi;
import javax.annotation.ParametersAreNonnullByDefault;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
@ApiStatus.NonExtendable
@ParametersAreNonnullByDefault
public interface SurfReflection {

  static SurfReflection get() {
    return SurfCoreApi.getCore().getReflection();
  }

  <T> T createProxy(Class<T> clazz, ClassLoader classLoader);

  default <T> T createProxy(Class<T> clazz) {
    return createProxy(clazz, checkNotNull(clazz, "clazz").getClassLoader());
  }
}
