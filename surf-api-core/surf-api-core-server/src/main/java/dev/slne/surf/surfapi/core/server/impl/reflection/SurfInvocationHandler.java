package dev.slne.surf.surfapi.core.server.impl.reflection;

import static com.google.common.base.Preconditions.checkState;

import dev.slne.surf.surfapi.core.api.reflection.annontation.Constructor;
import dev.slne.surf.surfapi.core.api.reflection.annontation.Field;
import dev.slne.surf.surfapi.core.api.reflection.annontation.Name;
import dev.slne.surf.surfapi.core.api.reflection.annontation.Static;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import javax.annotation.CheckForNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
@ApiStatus.Experimental
public final class SurfInvocationHandler<T> implements InvocationHandler {

  private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

  /**
   * The class representing the proxy
   */
  private final Class<T> proxyClass;

  /**
   * The class being proxied
   */
  private final Class<?> proxiedClass;

  // region Caches for different types of methods to improve performance
  private final Object2ObjectMap<Method, MethodHandle> normalCache;
  private final Object2ObjectMap<Method, MethodHandle> getterCache;
  private final Object2ObjectMap<Method, MethodHandle> setterCache;
  private final Object2ObjectMap<Method, MethodHandle> staticGetterCache;
  private final Object2ObjectMap<Method, MethodHandle> staticSetterCache;
  private final Object2ObjectMap<Method, MethodHandle> defaultCache;
  private final Object2ObjectMap<Method, java.lang.reflect.Field> staticFinalFieldSetterCache;
  private final Object2ObjectMap<Method, java.lang.reflect.Field> finalFieldSetterCache;
  // endregion

  public SurfInvocationHandler(Class<T> proxyClass, Class<?> proxiedClass) {
    this.proxyClass = proxyClass;
    this.proxiedClass = proxiedClass;

    // Initialize the caches for storing method handles
    this.normalCache = createMethodCache();
    this.getterCache = createMethodCache();
    this.setterCache = createMethodCache();
    this.staticGetterCache = createMethodCache();
    this.staticSetterCache = createMethodCache();
    this.defaultCache = createMethodCache();
    this.staticFinalFieldSetterCache = createMethodCache();
    this.finalFieldSetterCache = createMethodCache();

    // Cache methods from the interface for quick lookup
    cacheInterfaceMethods();
  }

  /**
   * Normalizes the type of a method handle to make it compatible with invocation
   *
   * @param handle the method handle to normalize
   * @return the normalized method handle
   **/
  private static MethodHandle normalizeMethodHandleType(@NotNull MethodHandle handle) {
    if (handle.type().parameterCount() == 0) {
      return handle.asType(MethodType.methodType(Object.class));
    }

    return handle.asSpreader(Object[].class, handle.type().parameterCount())
        .asType(MethodType.methodType(Object.class, Object[].class));
  }

  /**
   * Finds a field in the given class with the specified name
   *
   * @param clazz the class to search in
   * @param name  the name of the field
   * @return the field with the specified name
   * @throws NoSuchFieldException if the field with the specified name does not exist
   */
  private static java.lang.reflect.@NotNull Field findField(Class<?> clazz, String name)
      throws NoSuchFieldException {
    java.lang.reflect.Field field = FieldUtils.getField(clazz, name, true);

    if (field == null) {
      throw new NoSuchFieldException(name);
    }

    return field;
  }

  /**
   * Finds a constructor in the given class matching the parameter types of the method
   *
   * @param clazz  the class to search in
   * @param method the method to find the constructor for
   * @return the constructor matching the parameter types of the method
   * @throws NoSuchMethodException if the constructor does not exist
   */
  private static java.lang.reflect.@NotNull Constructor<?> findConstructor(
      @NotNull Class<?> clazz,
      @NotNull Method method
  ) throws NoSuchMethodException {
    java.lang.reflect.Constructor<?> constructor = clazz.getDeclaredConstructor(
        method.getParameterTypes());
    constructor.setAccessible(true);
    return constructor;
  }


  /**
   * Finds a method in the given class with a matching signature to the original method
   *
   * @param clazz            the class to search in
   * @param original         the original method to find a matching method for
   * @param nameAnnotation   the name annotation of the method
   * @param staticAnnotation the static annotation of the method
   * @return the method with a matching signature to the original method
   * @throws NoSuchMethodException if the method does not exist
   */
  private static @NotNull Method findMethod(
      Class<?> clazz,
      @NotNull Method original,
      @CheckForNull Name nameAnnotation,
      @CheckForNull Static staticAnnotation
  ) throws NoSuchMethodException {
    final Class<?>[] params = Arrays.stream(original.getParameters())
        .skip(staticAnnotation == null ? 1 : 0)
        .map(Parameter::getType)
        .toArray(Class[]::new);
    final String methodName = getMethodName(original, nameAnnotation, null, staticAnnotation, null);
    final Method method = MethodUtils.getMatchingAccessibleMethod(clazz, methodName, params);

    if (method == null) {
      throw new NoSuchMethodException(
          "method " + methodName + " with params " + Arrays.toString(params));
    }

    return method;
  }

  /**
   * Gets the appropriate name for a method, considering annotations
   *
   * @param method                the method to get the name for
   * @param nameAnnotation        the name annotation of the method
   * @param fieldAnnotation       the field annotation of the method
   * @param staticAnnotation      the static annotation of the method
   * @param constructorAnnotation the constructor annotation of the method
   * @return the appropriate name for the method
   */
  private static String getMethodName(
      Method method,
      @CheckForNull Name nameAnnotation,
      @CheckForNull Field fieldAnnotation,
      @CheckForNull Static staticAnnotation,
      @CheckForNull Constructor constructorAnnotation
  ) {
    if (nameAnnotation != null && !nameAnnotation.value().isBlank()) {
      return nameAnnotation.value();
    } else if (fieldAnnotation != null && !fieldAnnotation.name().isBlank()) {
      return fieldAnnotation.name();
    } else if (staticAnnotation != null && !staticAnnotation.name().isBlank()) {
      return staticAnnotation.name();
    } else if (constructorAnnotation != null) {
      return method.getReturnType().getSimpleName();
    } else {
      return method.getName();
    }
  }

  /**
   * Checks if the method has the expected number of parameters
   *
   * @param method   the method to check
   * @param expected the expected number of parameters
   */
  private static void checkPramCount(Method method, int expected) {
    checkState(method.getParameterCount() == expected, "method %s must have %s parameters", method,
        expected);
  }

  // region Utility methods to check if a method matches common Java object methods
  private static boolean isEqualsMethod(@NotNull Method method) {
    return method.getName().equals("equals") && method.getParameterCount() == 1
        && method.getParameterTypes()[0] == Object.class;
  }

  private static boolean isHashCodeMethod(@NotNull Method method) {
    return method.getName().equals("hashCode") && method.getParameterCount() == 0;
  }

  private static boolean isToStringMethod(@NotNull Method method) {
    return method.getName().equals("toString") && method.getParameterCount() == 0;
  }
  // endregion

  /**
   * Creates a synchronized cache for storing method handles
   *
   * @param <K> the type of the keys
   * @param <V> the type of the values
   * @return the synchronized cache
   */
  private static <K, V> @NotNull Object2ObjectMap<K, V> createMethodCache() {
    return Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
  }

  /**
   * A utility method that allows for sneaky exception throwing
   *
   * @param methodHandle the method handle to execute
   * @param <T>          the type of the result
   * @return the result of the method handle
   */
  private static <T> T sneaky(ExceptionalSupplier<T> methodHandle) {
    try {
      return methodHandle.get();
    } catch (Throwable throwable) {
      throw new RuntimeException(throwable);
    }
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // Handle equals method
    if (isEqualsMethod(method)) {
      return proxy == args[0];
    }

    // Handle hashCode method
    if (isHashCodeMethod(method)) {
      return System.identityHashCode(proxy);
    }

    // Handle toString method
    if (isToStringMethod(method)) {
      return ToStringBuilder.reflectionToString(proxy);
    }

    // Handle default methods
    if (method.isDefault()) {
      return handleDefault(proxy, method, args);
    }

    // Try invoking the method from the normal cache
    final Object normalInvoke = invoke(normalCache, method, args);
    if (normalInvoke != null) {
      return normalInvoke;
    }

    // Try invoking as a getter
    final Object getterInvoke = invoke(getterCache, method, args);
    if (getterInvoke != null) {
      return getterInvoke;
    }

    // Try invoking as a setter
    final Object setterInvoke = invoke(setterCache, method, args);
    if (setterInvoke != null) {
      return setterInvoke;
    }

    // Try invoking as a static getter
    final Object staticGetterInvoke = invoke(staticGetterCache, method, args);
    if (staticGetterInvoke != null) {
      return staticGetterInvoke;
    }

    // Try invoking as a static setter
    final Object staticSetterInvoke = invoke(staticSetterCache, method, args);
    if (staticSetterInvoke != null) {
      return staticSetterInvoke;
    }

    // Try invoking as a static field setter
    final Object staticFieldSetterInvoke = invokeStaticFieldSetter(method, args);
    if (staticFieldSetterInvoke != null) {
      return staticFieldSetterInvoke;
    }

    // Try invoking as a field setter
    final Object fieldSetterInvoke = invokeFieldSetter(method, args);
    if (fieldSetterInvoke != null) {
      return fieldSetterInvoke;
    }

    // If no invocation worked, throw an exception
    throw new UnsupportedOperationException("method " + method + " is not supported");
  }

  /**
   * Helper method to invoke a method handle from a cache
   *
   * @param cache  the cache to get the method handle from
   * @param method the method to invoke
   * @param args   the arguments to pass to the method
   * @return the result of the invocation
   * @throws Throwable if an error occurs during invocation
   */
  private @Nullable Object invoke(
      @NotNull Object2ObjectMap<Method, MethodHandle> cache,
      Method method,
      @CheckForNull Object[] args
  ) throws Throwable {
    final MethodHandle methodHandle = cache.get(method);
    if (methodHandle != null) {
      if (args == null) {
        return methodHandle.invokeExact();
      } else {
        return methodHandle.invokeWithArguments(args);
      }
    }

    return null;
  }

  private @Nullable Object invokeStaticFieldSetter(Method method, Object[] args) throws Throwable {
    final java.lang.reflect.Field field = staticFinalFieldSetterCache.get(method);

    if (field == null) {
      return null;
    }

    Util.setStaticFinalField(field, args[0]);
    return Void.TYPE;
  }

  private @Nullable Object invokeFieldSetter(Method method, Object @NotNull [] args) throws Throwable {
    final java.lang.reflect.Field field = finalFieldSetterCache.get(method);

    if (field == null) {
      return null;
    }

    Util.setFinalField(field, args[0], args[1]);
    return Void.TYPE;
  }

  /**
   * Handles default interface methods
   *
   * @param proxy  the proxy object
   * @param method the method to invoke
   * @param args   the arguments to pass to the method
   * @return the result of the invocation
   * @throws Throwable if an error occurs during invocation
   */
  private Object handleDefault(Object proxy, Method method, Object[] args) throws Throwable {
    final MethodHandle handle = defaultCache.computeIfAbsent(
        method,
        m -> normalizeMethodHandleType(sneaky(() ->
            MethodHandles.privateLookupIn(proxyClass, lookup)
                .findSpecial(
                    proxyClass,
                    method.getName(),
                    MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
                    proxyClass
                ).bindTo(proxy)
        ))
    );

    if (args == null) {
      return handle.invokeExact();
    } else {
      return handle.invokeExact(args);
    }
  }

  /**
   * Caches methods from the interface for quick lookup and invocation
   */
  private void cacheInterfaceMethods() {
    for (final Method method : proxyClass.getDeclaredMethods()) {
      if (isEqualsMethod(method) || isHashCodeMethod(method) || isToStringMethod(method)
          || method.isSynthetic() || method.isDefault()) {
        continue;
      }

      final Field fieldAnnotation = method.getDeclaredAnnotation(Field.class);
      final Static staticAnnotation = method.getDeclaredAnnotation(Static.class);
      final Constructor constructorAnnotation = method.getDeclaredAnnotation(Constructor.class);
      final Name nameAnnotation = method.getDeclaredAnnotation(Name.class);

      if (fieldAnnotation != null) {
        final String fieldName = getMethodName(method, nameAnnotation, fieldAnnotation,
            staticAnnotation, null);
        final java.lang.reflect.Field field = sneaky(() -> findField(proxiedClass, fieldName));

        // Handle final fields based on whether they are static or instance fields
        if (fieldAnnotation.overrideFinal()) {
          if (staticAnnotation != null) {
            if (fieldAnnotation.type() == Field.Type.GETTER) {
              checkPramCount(method, 0);
              staticGetterCache.put(method, sneaky(() -> lookup.unreflectGetter(field)));
            } else {
              checkPramCount(method, 1);
              staticFinalFieldSetterCache.put(method, field);
            }
          } else {
            if (fieldAnnotation.type() == Field.Type.GETTER) {
              checkPramCount(method, 1);
              getterCache.put(method, sneaky(() -> lookup.unreflectGetter(field)));
            } else {
              checkPramCount(method, 2);
              finalFieldSetterCache.put(method, field);
            }
          }
        } else {
          final MethodHandle methodHandle = sneaky(() -> lookup.unreflectGetter(field));
          if (staticAnnotation != null) {
            if (fieldAnnotation.type() == Field.Type.GETTER) {
              checkPramCount(method, 0);
              staticGetterCache.put(method,
                  methodHandle.asType(MethodType.methodType(Object.class)));
            } else {
              checkPramCount(method, 1);
              staticSetterCache.put(method,
                  methodHandle.asType(MethodType.methodType(Object.class)));
            }
          } else {
            if (fieldAnnotation.type() == Field.Type.GETTER) {
              checkPramCount(method, 1);
              getterCache.put(method,
                  methodHandle.asType(MethodType.methodType(Object.class, Object.class)));
            } else {
              checkPramCount(method, 2);
              setterCache.put(method,
                  methodHandle.asType(MethodType.methodType(Object.class, Object.class)));
            }
          }
        }
      } else if (constructorAnnotation != null) {
        normalCache.put(method, normalizeMethodHandleType(
            sneaky(() -> lookup.unreflectConstructor(findConstructor(proxiedClass, method)))));
      } else {
        checkState(staticAnnotation != null || method.getParameterCount() > 0,
            "method %s must have at least one parameter %s %s", method, staticAnnotation,
            method.getParameterCount());
        normalCache.put(method, normalizeMethodHandleType(sneaky(() -> lookup.unreflect(
            findMethod(proxiedClass, method, nameAnnotation, staticAnnotation)))));
      }
    }
  }

  @FunctionalInterface
  interface ExceptionalSupplier<T> {

    T get() throws Throwable;
  }
}
