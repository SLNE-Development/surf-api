package dev.slne.surf.surfapi.core.server.impl.reflection;

import dev.slne.surf.surfapi.core.api.reflection.Field.Type;
import dev.slne.surf.surfapi.core.api.reflection.Name;
import dev.slne.surf.surfapi.core.api.reflection.Static;
import dev.slne.surf.surfapi.core.api.util.SurfUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@NullMarked
public final class SurfInvocationHandlerJava<T> implements InvocationHandler {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final Object[] EMPTY_ARGS = new Object[0];

    private final Class<T> proxyClass;
    private final Class<?> proxiedClass;
    private final Object2ObjectMap<Method, Invokable> cache;
    private final Map<Method, MethodHandle> defaultCache = new ConcurrentHashMap<>();

    public SurfInvocationHandlerJava(Class<T> proxyClass, Class<?> proxiedClass) {
        this.proxyClass = proxyClass;
        this.proxiedClass = proxiedClass;
        this.cache = buildCache();
    }

    @Override
    public @Nullable Object invoke(final Object proxy, final Method method,
                                   final Object @Nullable [] args)
            throws Throwable {
        if (isEqualsMethod(method)) {
            return Objects.equals(proxy, Objects.requireNonNull(args)[0]);
        } else if (isHashCodeMethod(method)) {
            return System.identityHashCode(proxy);
        } else if (isToStringMethod(method)) {
            return ToStringBuilder.reflectionToString(proxy);
        } else if (method.isDefault()) {
            return invokeDefault(proxy, method, args);
        } else {
            final Invokable invokable = cache.get(method);
            if (invokable == null) {
                throw new IllegalStateException("No handler cached for " + method.getName());
            } else {
                return invokable.invoke(args != null ? args : EMPTY_ARGS);
            }
        }
    }

    private @Nullable Object invokeDefault(final Object proxy, final Method method,
                                           final Object @Nullable [] args)
            throws Throwable {
        final MethodHandle handle = defaultCache.computeIfAbsent(method,
                m -> sneaky(() -> normalizeMethodHandleType(
                        MethodHandles.privateLookupIn(proxyClass, LOOKUP)
                                .findSpecial(
                                        proxyClass,
                                        m.getName(),
                                        MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
                                        proxyClass
                                )
                                .bindTo(proxy)
                )));

        return args == null ? handle.invokeExact() : handle.invokeExact(args);
    }

    private Object2ObjectMap<Method, Invokable> buildCache() {
        return Arrays.stream(proxyClass.getDeclaredMethods())
                .filter(m -> !m.isSynthetic())
                .filter(m -> !m.isDefault())
                .filter(m -> !isEqualsHashOrToStringMethod(m))
                .collect(Collectors.toMap(Function.identity(), this::createInvokable, (e1, e2) -> e1,
                        Object2ObjectOpenHashMap::new));
    }

    private Invokable createInvokable(final Method method) {
        final var fieldAnnotation = method.getDeclaredAnnotation(
                dev.slne.surf.surfapi.core.api.reflection.Field.class);
        final var staticAnnotation = method.getDeclaredAnnotation(Static.class);
        final var constructorAnnotation = method.getDeclaredAnnotation(
                dev.slne.surf.surfapi.core.api.reflection.Constructor.class);
        final var nameAnnotation = method.getDeclaredAnnotation(Name.class);
        final var privateLookup = sneaky(() -> MethodHandles.privateLookupIn(proxiedClass, LOOKUP));

        if (fieldAnnotation != null) {
            final String fieldName = getMethodName(method, nameAnnotation, fieldAnnotation,
                    staticAnnotation, constructorAnnotation);
            final Field field = sneaky(() -> findField(proxiedClass, fieldName));
            final boolean isGetter = fieldAnnotation.type() == Type.GETTER;
            final MethodHandle handleGetter =
                    isGetter ? sneaky(() -> privateLookup.unreflectGetter(field)) : null;
            final MethodHandle handleSetter = !isGetter && !fieldAnnotation.overrideFinal()
                    ? sneaky(() -> privateLookup.unreflectSetter(field)) : null;

            if (isGetter) {
                checkParamCount(method, staticAnnotation != null ? 0 : 1);
                final boolean hasParams = staticAnnotation == null; // instance parameter
                return new HandleInvokable(normalizeMethodHandleType(handleGetter), hasParams);
            } else {
                if (fieldAnnotation.overrideFinal()) {
                    checkParamCount(method, staticAnnotation != null ? 1 : 2);
                    return new ReflectionSetterInvokable(field, staticAnnotation != null);
                }

                checkParamCount(method, staticAnnotation != null ? 1 : 2);
                final boolean hasParams = true; // setter always has params
                return new HandleInvokable(normalizeMethodHandleType(handleSetter), hasParams);
            }
        }

        if (constructorAnnotation != null) {
            final var handle = sneaky(
                    () -> privateLookup.unreflectConstructor(findConstructor(proxiedClass, method)));
            final boolean hasParams = handle.type().parameterCount() > 0;
            return new HandleInvokable(normalizeMethodHandleType(handle), hasParams);
        }

        if (staticAnnotation == null && method.getParameterCount() == 0) {
            throw new IllegalStateException(
                    "Instance method '" + method.getName() + "' must have a receiver parameter");
        }

        final Method target = sneaky(
                () -> findMethod(proxiedClass, method, nameAnnotation, staticAnnotation));
        final MethodHandle handle = sneaky(() -> privateLookup.unreflect(target));
        final boolean hasParams = handle.type().parameterCount() > 0;
        return new HandleInvokable(normalizeMethodHandleType(handle), hasParams);
    }

    private static MethodHandle normalizeMethodHandleType(final MethodHandle handle) {
        if (handle.type().parameterCount() == 0) {
            return handle.asType(MethodType.methodType(Object.class));
        } else {
            return handle.asSpreader(Object[].class, handle.type().parameterCount())
                    .asType(MethodType.methodType(Object.class, Object[].class));
        }
    }

    private static Field findField(final Class<?> clazz, final String name)
            throws NoSuchFieldException {
        final Field field = FieldUtils.getField(clazz, name, true);
        if (field == null) {
            throw new NoSuchFieldException(name);
        }
        return field;
    }

    private static Constructor<?> findConstructor(final Class<?> clazz, final Method method)
            throws NoSuchMethodException {
        final Constructor<?> constructor = clazz.getDeclaredConstructor(method.getParameterTypes());
        constructor.setAccessible(true);
        return constructor;
    }

    private static Method findMethod(
            final Class<?> clazz,
            final Method original,
            final Name nameAnnotation,
            final @Nullable Static staticAnnotation
    ) throws NoSuchMethodException {
        final int paramOffset = staticAnnotation == null ? 1 : 0;  // instance param first if not static
        final Class<?>[] paramTypes = Arrays.copyOfRange(original.getParameterTypes(), paramOffset,
                original.getParameterCount());
        final String methodName = getMethodName(original, nameAnnotation, null, staticAnnotation, null);
        Method method = MethodUtils.getMatchingMethod(clazz, methodName, paramTypes);

        if (method == null && isAllObjectParams(paramTypes)) {
            method = findMethodByNameAndParamCount(clazz, methodName, paramTypes.length);
        }

        if (method != null) {
            method.setAccessible(true);
        }

        if (method == null) {
            throw new NoSuchMethodException(
                    "Method '" + methodName + "' with params " + Arrays.toString(paramTypes));
        }

        return method;
    }

    private static @Nullable Method findMethodByNameAndParamCount(
            final Class<?> clazz,
            final String methodName,
            final int paramCount
    ) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == paramCount) {
                return method;
            }
        }
        return null;
    }

    private static boolean isAllObjectParams(final Class<?>[] paramTypes) {
        for (Class<?> type : paramTypes) {
            if (type != Object.class) {
                return false;
            }
        }
        return true;
    }

    private static String getMethodName(
            final Method method,
            final @Nullable Name nameAnnotation,
            final dev.slne.surf.surfapi.core.api.reflection.@Nullable Field fieldAnnotation,
            final @Nullable Static staticAnnotation,
            final dev.slne.surf.surfapi.core.api.reflection.@Nullable Constructor constructorAnnotation
    ) {
        // when block converted to if-else structure
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

    private static void checkParamCount(final Method method, final int expected) {
        if (method.getParameterCount() != expected) {
            throw new IllegalStateException(
                    "Method " + method.getName() + " must have " + expected + " parameters, found "
                            + method.getParameterCount());
        }
    }

    private static boolean isEqualsMethod(final Method method) {
        return "equals".equals(method.getName()) && method.getParameterCount() == 1
                && method.getParameterTypes()[0] == Object.class;
    }

    private static boolean isHashCodeMethod(final Method method) {
        return "hashCode".equals(method.getName()) && method.getParameterCount() == 0;
    }

    private static boolean isToStringMethod(final Method method) {
        return "toString".equals(method.getName()) && method.getParameterCount() == 0;
    }

    private static boolean isEqualsHashOrToStringMethod(final Method method) {
        return isEqualsMethod(method) || isHashCodeMethod(method) || isToStringMethod(method);
    }

    private sealed interface Invokable permits HandleInvokable, ReflectionSetterInvokable {

        @Nullable
        Object invoke(final Object[] args) throws Throwable;
    }


    private record HandleInvokable(MethodHandle handle, boolean hasParams) implements Invokable {

        @Override
        public @Nullable Object invoke(final Object[] args) throws Throwable {
            if (hasParams) {
                return handle.invokeExact(args);
            } else {
                return handle.invokeExact();
            }
        }
    }

    private record ReflectionSetterInvokable(Field field, boolean isStatic) implements Invokable {

        @Override
        public @Nullable Object invoke(Object[] args) throws Throwable {
            if (isStatic) {
                SurfUtil.setStaticFinalField(field, args[0]);
            } else {
                SurfUtil.setFinalField(field, args[0], args[1]);
            }
            return null;
        }
    }

    private static <T> T sneaky(final ExceptionalSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    private interface ExceptionalSupplier<T> {

        T get() throws Throwable;
    }
}
