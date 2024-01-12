package dev.slne.surf.surfapi.core.server.impl.reflection;

import dev.slne.surf.surfapi.core.api.reflection.SurfReflection;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Proxy;

import static com.google.common.base.Preconditions.*;

@ApiStatus.Internal
@ApiStatus.Experimental
@ParametersAreNonnullByDefault
public final class SurfReflectionImpl implements SurfReflection {
    @Override
    public <T> T createProxy(Class<T> clazz, ClassLoader classLoader) {
        checkNotNull(clazz, "clazz");
        checkNotNull(classLoader, "classLoader");
        checkArgument(clazz.isInterface(), "clazz must be an interface");

        return (T) Proxy.newProxyInstance(
                classLoader,
                new Class[]{clazz},
                new SurfInvocationHandler<>(clazz)
        );
    }
}
