package dev.slne.surf.surfapi.core.api.reflection;

import dev.slne.surf.surfapi.core.api.SurfCoreApi;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.google.common.base.Preconditions.*;

@ApiStatus.Experimental
@ApiStatus.NonExtendable
@ParametersAreNonnullByDefault
public interface SurfReflection {

    <T> T createProxy(Class<T> clazz, ClassLoader classLoader);

    default <T> T createProxy(Class<T> clazz) {
        return createProxy(clazz, checkNotNull(clazz, "clazz").getClassLoader());
    }

    static SurfReflection get() {
        return SurfCoreApi.getCore().getReflection();
    }
}
