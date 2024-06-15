package dev.slne.surf.surfapi.core.server.impl.reflection;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import dev.slne.surf.surfapi.core.api.reflection.SurfReflection;
import dev.slne.surf.surfapi.core.api.reflection.annontation.SurfProxy;
import java.lang.reflect.Proxy;
import javax.annotation.ParametersAreNonnullByDefault;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@ApiStatus.Experimental
@ParametersAreNonnullByDefault
public final class SurfReflectionImpl implements SurfReflection {

  @Override
  public <T> T createProxy(Class<T> clazz, ClassLoader classLoader) {
    checkNotNull(clazz, "clazz");
    checkNotNull(classLoader, "classLoader");
    checkArgument(clazz.isInterface(), "clazz must be an interface");
    checkState(clazz.isAnnotationPresent(SurfProxy.class),
        "clazz must be annotated with @SurfProxy");

    final SurfProxy surfProxy = clazz.getAnnotation(SurfProxy.class);
    final boolean useQualifiedClassName = surfProxy.value().equals(void.class);
    checkState(!useQualifiedClassName || !surfProxy.qualifiedName().isEmpty(),
        "clazz must have a value or qualifiedName in @SurfProxy");
    final Class<?> proxyClass;

    if (!useQualifiedClassName) {
      proxyClass = surfProxy.value();
    } else {
      try {
        proxyClass = Class.forName(surfProxy.qualifiedName());
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException("Could not find class " + surfProxy.qualifiedName(), e);
      }
    }

    return (T) Proxy.newProxyInstance(
        classLoader,
        new Class[]{clazz},
        new SurfInvocationHandler<>(clazz, proxyClass)
    );
  }
}
