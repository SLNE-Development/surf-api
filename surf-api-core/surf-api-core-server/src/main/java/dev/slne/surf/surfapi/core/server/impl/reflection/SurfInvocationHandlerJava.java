package dev.slne.surf.surfapi.core.server.impl.reflection;

import dev.slne.surf.surfapi.core.api.reflection.Field.Type;
import dev.slne.surf.surfapi.core.api.reflection.Name;
import dev.slne.surf.surfapi.core.api.reflection.Static;
import dev.slne.surf.surfapi.core.api.util.SurfUtil;
import dev.slne.surf.surfapi.core.server.impl.reflection.reflection.ProxyCreationException;
import dev.slne.surf.surfapi.core.server.impl.reflection.reflection.ProxyInvocationException;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.*;
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
    private final Object2ObjectMap<Method, @Nullable Invokable> cache;
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
            return "SurfProxy[" + proxiedClass.getName() + "@" + Integer.toHexString(System.identityHashCode(proxy)) + "]";
        } else if (method.isDefault()) {
            return invokeDefault(proxy, method, args);
        } else {
            final Invokable invokable = cache.get(method);
            if (invokable == null) {
                throw new ProxyInvocationException("No handler found for method '" + method.getName() + "' in proxy for class " + proxiedClass.getName());
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

        validateAnnotationCombination(method, fieldAnnotation, staticAnnotation, constructorAnnotation);

        final MethodHandles.Lookup privateLookup;
        try {
            privateLookup = MethodHandles.privateLookupIn(proxiedClass, LOOKUP);
        } catch (IllegalAccessException e) {
            throw new ProxyCreationException(
                    "Cannot access class " + proxiedClass.getName() +
                            ". Module may not be open for reflection. " +
                            "Consider adding 'opens " + proxiedClass.getPackageName() +
                            ";' to your module-info.java", e);
        }

        if (fieldAnnotation != null) {
            return createFieldInvokable(method, fieldAnnotation, staticAnnotation, nameAnnotation, privateLookup);
        }

        if (constructorAnnotation != null) {
            final var handle = sneaky(
                    () -> privateLookup.unreflectConstructor(findConstructor(proxiedClass, method)));
            final boolean hasParams = handle.type().parameterCount() > 0;
            return new HandleInvokable(normalizeMethodHandleType(handle), hasParams);
        }

        if (staticAnnotation == null && method.getParameterCount() == 0) {
            throw new ProxyCreationException(
                    "Instance method '" + method.getName() + "' must have at least one parameter " +
                            "(the instance object). Add @Static if this should be a static method call.");
        }

        final Method target = sneaky(
                () -> findMethod(proxiedClass, method, nameAnnotation, staticAnnotation));
        final MethodHandle handle = sneaky(() -> privateLookup.unreflect(target));
        final boolean hasParams = handle.type().parameterCount() > 0;
        return new HandleInvokable(normalizeMethodHandleType(handle), hasParams);
    }

    private Invokable createFieldInvokable(
            final Method method,
            final dev.slne.surf.surfapi.core.api.reflection.Field fieldAnnotation,
            final @Nullable Static staticAnnotation,
            final @Nullable Name nameAnnotation,
            final MethodHandles.Lookup privateLookup
    ) {
        final String fieldName = getMethodName(method, nameAnnotation, fieldAnnotation,
                staticAnnotation, null);
        final Field field = sneaky(() -> findField(proxiedClass, fieldName));
        final boolean isGetter = fieldAnnotation.type() == Type.GETTER;

        if (isGetter) {
            final java.lang.invoke.MethodHandle handleGetter = sneaky(() -> privateLookup.unreflectGetter(field));
            final boolean hasParams = staticAnnotation == null;
            return new HandleInvokable(normalizeMethodHandleType(handleGetter), hasParams);
        } else {
            if (fieldAnnotation.overrideFinal()) {
                return new VarHandleSetterInvokable(field, staticAnnotation != null, privateLookup);
            }

            final java.lang.invoke.MethodHandle handleSetter = sneaky(() -> privateLookup.unreflectSetter(field));
            return new HandleInvokable(normalizeMethodHandleType(handleSetter), true);
        }
    }

    private static void validateAnnotationCombination(
            final Method method,
            final dev.slne.surf.surfapi.core.api.reflection.@Nullable Field fieldAnnotation,
            final @Nullable Static staticAnnotation,
            final dev.slne.surf.surfapi.core.api.reflection.@Nullable Constructor constructorAnnotation
    ) {
        int annotationCount = 0;
        if (fieldAnnotation != null) annotationCount++;
        if (constructorAnnotation != null) annotationCount++;

        if (annotationCount > 1) {
            throw new ProxyCreationException(
                    "Method '" + method.getName() + "' has multiple incompatible annotations. " +
                            "Use only one of: @Field, @Constructor");
        }

        if (constructorAnnotation != null && staticAnnotation != null) {
            throw new ProxyCreationException(
                    "Method '" + method.getName() + "' cannot have both @Constructor and @Static");
        }

        if (fieldAnnotation != null) {
            final boolean isStatic = staticAnnotation != null;
            final boolean isGetter = fieldAnnotation.type() == Type.GETTER;
            final int paramCount = method.getParameterCount();
            final int expectedParams = isStatic ? (isGetter ? 0 : 1) : (isGetter ? 1 : 2);

            if (paramCount != expectedParams) {
                throw new ProxyCreationException(
                        "Method '" + method.getName() + "' has invalid parameter count. " +
                                "Expected " + expectedParams + " parameters for " +
                                (isStatic ? "static " : "instance ") +
                                (isGetter ? "getter" : "setter") + ", found " + paramCount);
            }
        }
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
        Field field = null;
        Class<?> current = clazz;

        while (current != null && field == null) {
            try {
                field = current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }

        if (field == null) {
            throw new NoSuchFieldException("Field '" + name + "' not found in class " +
                    clazz.getName() + " or its superclasses");
        }

        field.setAccessible(true);
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
        Method method = findMethodExact(clazz, methodName, paramTypes);

        if (method == null && isAllObjectParams(paramTypes)) {
            method = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(m -> m.getName().equals(methodName))
                    .filter(m -> m.getParameterCount() == paramTypes.length)
                    .findFirst()
                    .orElse(null);
        }

        if (method != null) {
            method.setAccessible(true);
            return method;
        }

        final String availableMethods = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName))
                .map(m -> methodName + "(" +
                        Arrays.stream(m.getParameterTypes())
                                .map(Class::getSimpleName)
                                .collect(Collectors.joining(", ")) + ")")
                .collect(Collectors.joining(", "));

        throw new NoSuchMethodException(
                "Method '" + methodName + "' with parameters [" +
                        Arrays.stream(paramTypes).map(Class::getSimpleName).collect(Collectors.joining(", ")) +
                        "] not found in class " + clazz.getName() +
                        (availableMethods.isEmpty() ? "" : ". Available methods with same name: " + availableMethods));
    }

    private static @Nullable Method findMethodExact(final Class<?> clazz, final String name,
                                                    final Class<?>[] paramTypes) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name, paramTypes);
            } catch (NoSuchMethodException e) {
                for (Method method : current.getDeclaredMethods()) {
                    if (method.getName().equals(name) &&
                            Arrays.equals(method.getParameterTypes(), paramTypes)) {
                        return method;
                    }
                }
                current = current.getSuperclass();
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

    private sealed interface Invokable permits HandleInvokable, ReflectionSetterInvokable, VarHandleSetterInvokable {

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

    private record VarHandleSetterInvokable(Field field, boolean isStatic,
                                            MethodHandles.Lookup lookup) implements Invokable {
        @Override
        public @Nullable Object invoke(Object[] args) throws ProxyInvocationException {
            if (Modifier.isFinal(field.getModifiers())) {
                return setFieldViaUnsafe(args);
            }

            try {
                VarHandle varHandle = isStatic
                        ? lookup.findStaticVarHandle(field.getDeclaringClass(), field.getName(), field.getType())
                        : lookup.findVarHandle(field.getDeclaringClass(), field.getName(), field.getType());

                if (isStatic) {
                    varHandle.set(args[0]);
                } else {
                    varHandle.set(args[0], args[1]);
                }
                return null;
            } catch (Exception e) {
                try {
                    return setFieldViaUnsafe(args);
                } catch (Exception e2) {
                    // If we reach this point, it means we failed to set the final field using both VarHandle and Unsafe.
                    String moduleName = field.getDeclaringClass().getModule().getName();
                    String errorMsg = buildFinalFieldErrorMessage(moduleName);
                    throw new ProxyInvocationException(errorMsg, e2);
                }
            }
        }

        @SuppressWarnings("removal")
        private @Nullable Object setFieldViaUnsafe(Object[] args) throws ProxyInvocationException {
            sun.misc.Unsafe unsafe = SurfUtil.getUnsafe();

            long offset = isStatic
                    ? unsafe.staticFieldOffset(field)
                    : unsafe.objectFieldOffset(field);

            Object value = isStatic ? args[0] : args[1];
            Object target = isStatic ? unsafe.staticFieldBase(field) : args[0];

            Class<?> type = field.getType();
            if (type == int.class) {
                unsafe.putInt(target, offset, (Integer) value);
            } else if (type == long.class) {
                unsafe.putLong(target, offset, (Long) value);
            } else if (type == boolean.class) {
                unsafe.putBoolean(target, offset, (Boolean) value);
            } else if (type == byte.class) {
                unsafe.putByte(target, offset, (Byte) value);
            } else if (type == short.class) {
                unsafe.putShort(target, offset, (Short) value);
            } else if (type == char.class) {
                unsafe.putChar(target, offset, (Character) value);
            } else if (type == float.class) {
                unsafe.putFloat(target, offset, (Float) value);
            } else if (type == double.class) {
                unsafe.putDouble(target, offset, (Double) value);
            } else {
                unsafe.putObject(target, offset, value);
            }

            return null;
        }

        private String buildFinalFieldErrorMessage(@Nullable String moduleName) {
            String javaVersion = System.getProperty("java.version");
            String fieldInfo = (isStatic ? "static " : "") + "final field '" +
                    field.getName() + "' in class " +
                    field.getDeclaringClass().getName();

            StringBuilder sb = new StringBuilder();
            sb.append("Cannot set ").append(fieldInfo).append(".\n\n");
            sb.append("Java ").append(javaVersion).append(" enforces final field immutability (JEP 500).\n\n");
            sb.append("SHORT-TERM SOLUTIONS:\n");
            sb.append("1. Allow final field mutation temporarily:\n");
            sb.append("   --illegal-final-field-mutation=allow\n\n");
            sb.append("2. Get warnings but allow mutation (current default in JDK 26):\n");
            sb.append("   --illegal-final-field-mutation=warn\n\n");
            sb.append("RECOMMENDED SOLUTION:\n");
            sb.append("Enable final field mutation for your module:\n");

            if (moduleName != null && !moduleName.isEmpty()) {
                sb.append("   --enable-final-field-mutation=").append(moduleName).append("\n\n");
            } else {
                sb.append("   --enable-final-field-mutation=ALL-UNNAMED\n\n");
            }

            sb.append("Add this to:\n");
            sb.append("- Command line: java --enable-final-field-mutation=... -jar app.jar\n");
            sb.append("- Environment: export JDK_JAVA_OPTIONS=\"--enable-final-field-mutation=...\"\n");
            sb.append("- JAR Manifest: Enable-Final-Field-Mutation: ALL-UNNAMED\n\n");

            sb.append("IMPORTANT: This capability will be further restricted in future Java releases.\n");
            sb.append("Consider refactoring to avoid final field mutation where possible.\n");
            sb.append("See: https://openjdk.org/jeps/500");

            return sb.toString();
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
