package dev.slne.surf.surfapi.core.server.impl.reflection;

import dev.slne.surf.surfapi.core.api.reflection.annontation.Constructor;
import dev.slne.surf.surfapi.core.api.reflection.annontation.Field;
import dev.slne.surf.surfapi.core.api.reflection.annontation.Name;
import dev.slne.surf.surfapi.core.api.reflection.annontation.Static;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.CheckForNull;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

import static com.google.common.base.Preconditions.*;

@ApiStatus.Internal
@ApiStatus.Experimental
public final class SurfInvocationHandler<T> implements InvocationHandler {

    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    private final Class<T> proxyClass;
    private final Object2ObjectMap<Method, MethodHandle> normalCache;
    private final Object2ObjectMap<Method, MethodHandle> getterCache;
    private final Object2ObjectMap<Method, MethodHandle> setterCache;
    private final Object2ObjectMap<Method, MethodHandle> staticGetterCache;
    private final Object2ObjectMap<Method, MethodHandle> staticSetterCache;
    private final Object2ObjectMap<Method, MethodHandle> defaultCache;

    public SurfInvocationHandler(Class<T> proxyClass) {
        this.proxyClass = proxyClass;

        this.normalCache = createMethodCache();
        this.getterCache = createMethodCache();
        this.setterCache = createMethodCache();
        this.staticGetterCache = createMethodCache();
        this.staticSetterCache = createMethodCache();
        this.defaultCache = createMethodCache();

        cacheInterfaceMethods();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (isEqualsMethod(method)) {
            return proxy == args[0];
        }

        if (isHashCodeMethod(method)) {
            return System.identityHashCode(proxy);
        }

        if (isToStringMethod(method)) {
            return ToStringBuilder.reflectionToString(proxy);
        }

        if (method.isDefault()) {
            return handleDefault(proxy, method, args);
        }

        final Object normalInvoke = invoke(normalCache, method, args);
        if (normalInvoke != null) {
            return normalInvoke;
        }

        final Object getterInvoke = invoke(getterCache, method, args);
        if (getterInvoke != null) {
            return getterInvoke;
        }

        final Object setterInvoke = invoke(setterCache, method, args);
        if (setterInvoke != null) {
            return setterInvoke;
        }

        final Object staticGetterInvoke = invoke(staticGetterCache, method, args);
        if (staticGetterInvoke != null) {
            return staticGetterInvoke;
        }

        final Object staticSetterInvoke = invoke(staticSetterCache, method, args);
        if (staticSetterInvoke != null) {
            return staticSetterInvoke;
        }

        throw new UnsupportedOperationException("method " + method + " is not supported");
    }

    private Object invoke(Object2ObjectMap<Method, MethodHandle> cache, Method method, @CheckForNull Object[] args) throws Throwable {
        final MethodHandle methodHandle = cache.get(method);
        if (methodHandle != null) {
            if (args == null) {
                return methodHandle.invokeExact();
            } else {
                return methodHandle.invokeExact(args);
            }
        }

        return null;
    }

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

    private void cacheInterfaceMethods() {
        for (Method method : proxyClass.getDeclaredMethods()) {
            if (isEqualsMethod(method) || isHashCodeMethod(method) || isToStringMethod(method) || method.isSynthetic() || method.isDefault()) {
                continue;
            }

            final Field fieldAnnotation = method.getDeclaredAnnotation(Field.class);
            final Static staticAnnotation = method.getDeclaredAnnotation(Static.class);
            final Constructor constructorAnnotation = method.getDeclaredAnnotation(Constructor.class);
            final Name nameAnnotation = method.getDeclaredAnnotation(Name.class);

            if (fieldAnnotation != null) {
                final MethodHandle methodHandle = sneaky(() -> lookup.unreflectGetter(findField(proxyClass, getMethodName(method, nameAnnotation, fieldAnnotation, staticAnnotation, null))));

                if (staticAnnotation != null) {
                    checkPramCount(method, 0);
                    if (fieldAnnotation.type() == Field.Type.GETTER) {
                        staticGetterCache.put(method, methodHandle.asType(MethodType.methodType(Object.class)));
                    } else {
                        staticSetterCache.put(method, methodHandle.asType(MethodType.methodType(Object.class)));
                    }
                } else {
                    checkPramCount(method, 1);
                    if (fieldAnnotation.type() == Field.Type.GETTER) {
                        getterCache.put(method, methodHandle.asType(MethodType.methodType(Object.class, Object.class)));
                    } else {
                        setterCache.put(method, methodHandle.asType(MethodType.methodType(Object.class, Object.class)));
                    }
                }
            } else if (constructorAnnotation != null) {
                normalCache.put(method, normalizeMethodHandleType(sneaky(() -> lookup.unreflectConstructor(findConstructor(proxyClass, method)))));
            } else {
                checkState(staticAnnotation != null || method.getParameterCount() > 0, "method %s must have at least one parameter", method);
                normalCache.put(method, normalizeMethodHandleType(sneaky(() -> lookup.unreflect(findMethod(proxyClass, method, nameAnnotation, staticAnnotation)))));
            }
        }
    }

    private static MethodHandle normalizeMethodHandleType(MethodHandle handle) {
        if (handle.type().parameterCount() == 0) {
            return handle.asType(MethodType.methodType(Object.class));
        }

        return handle.asSpreader(Object[].class, handle.type().parameterCount()).asType(MethodType.methodType(Object.class, Object[].class));
    }

    private static java.lang.reflect.Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        final java.lang.reflect.Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static java.lang.reflect.Constructor<?> findConstructor(Class<?> clazz, Method method) throws NoSuchMethodException {
        java.lang.reflect.Constructor<?> constructor = clazz.getDeclaredConstructor(method.getParameterTypes());
        constructor.setAccessible(true);
        return constructor;
    }

    private static Method findMethod(Class<?> clazz,
                                     Method original,
                                     @CheckForNull Name nameAnnotation,
                                     @CheckForNull Static staticAnnotation) throws NoSuchMethodException {
        final Class<?>[] params = Arrays.stream(original.getParameters())
                .skip(staticAnnotation != null ? 1 : 0)
                .map(Parameter::getType)
                .toArray(Class[]::new);
        final Method method = clazz.getDeclaredMethod(getMethodName(original, nameAnnotation, null, staticAnnotation, null), params);

        method.setAccessible(true);

        return method;
    }

    private static String getMethodName(Method method,
                                        @CheckForNull Name nameAnnotation,
                                        @CheckForNull Field fieldAnnotation,
                                        @CheckForNull Static staticAnnotation,
                                        @CheckForNull Constructor constructorAnnotation) {
        if (nameAnnotation != null) {
            return nameAnnotation.value();
        } else if (fieldAnnotation != null) {
            return fieldAnnotation.name();
        } else if (staticAnnotation != null) {
            return staticAnnotation.name();
        } else if (constructorAnnotation != null) {
            return method.getReturnType().getSimpleName();
        } else {
            return method.getName();
        }
    }

    private static void checkPramCount(Method method, int expected) {
        checkState(method.getParameterCount() == expected, "method %s must have %s parameters", method, expected);
    }

    private static boolean isEqualsMethod(Method method) {
        return method.getName().equals("equals") && method.getParameterCount() == 1 && method.getParameterTypes()[0] == Object.class;
    }

    private static boolean isHashCodeMethod(Method method) {
        return method.getName().equals("hashCode") && method.getParameterCount() == 0;
    }

    private static boolean isToStringMethod(Method method) {
        return method.getName().equals("toString") && method.getParameterCount() == 0;
    }

    private static <K, V> Object2ObjectMap<K, V> createMethodCache() {
        return Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
    }

    private static <T> T sneaky(ExceptionalSupplier<T> methodHandle) {
        try {
            return methodHandle.get();
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    @FunctionalInterface
    interface ExceptionalSupplier<T> {
        T get() throws Throwable;
    }
}
