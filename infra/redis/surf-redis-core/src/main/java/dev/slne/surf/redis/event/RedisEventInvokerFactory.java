package dev.slne.surf.redis.event;

import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.io.InputStream;
import java.lang.constant.ConstantDescs;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

@NullMarked
final class RedisEventInvokerFactory {

    private static final byte[] TEMPLATE_CLASS_BYTES;
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    static {
        try (final InputStream is = RedisEventInvokerFactory.class.getResourceAsStream(RedisEventInvokerTemplate.class.getSimpleName() + ".class")) {
            Objects.requireNonNull(is, "RedisEventInvokerTemplate.class not found");
            TEMPLATE_CLASS_BYTES = is.readAllBytes();
        } catch (IOException e) {
            throw new AssertionError("Failed to load RedisEventInvokerTemplate.class", e);
        }
    }

    private RedisEventInvokerFactory() {
        throw new UnsupportedOperationException("RedisEventInvokerFactory is a utility class and cannot be instantiated");
    }

    public static RedisEventInvoker create(final Object listener, final Method method, final Class<? extends RedisEvent> redisEventClass) {
        try {
            final Class<?> listenerClass = listener.getClass();
            final MethodHandles.Lookup privateLookupIn = MethodHandles.privateLookupIn(listenerClass, LOOKUP);
            final List<Object> classData = List.of(listener, redisEventClass, method, privateLookupIn);

            final MethodHandles.Lookup hiddenClassLookup = LOOKUP.defineHiddenClassWithClassData(TEMPLATE_CLASS_BYTES, classData, true);

            return hiddenClassLookup.lookupClass()
                    .asSubclass(RedisEventInvoker.class)
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to create RedisEventInvoker for " + method, e);
        }
    }

    record ClassData(Method method, MethodHandle methodHandle, Class<? extends RedisEvent> redisEventClass) {
    }

    static ClassData classData(final MethodHandles.Lookup lookup) {
        try {
            final Object listener = MethodHandles.classDataAt(lookup, ConstantDescs.DEFAULT_NAME, Object.class, 0);
            final Class<?> redisEventClass = MethodHandles.classDataAt(lookup, ConstantDescs.DEFAULT_NAME, Class.class, 1);
            final Method method = MethodHandles.classDataAt(lookup, ConstantDescs.DEFAULT_NAME, Method.class, 2);
            final MethodHandles.Lookup privateLookupIn = MethodHandles.classDataAt(lookup, ConstantDescs.DEFAULT_NAME, MethodHandles.Lookup.class, 3);

            final MethodHandle methodHandle = privateLookupIn.unreflect(method)
                    .bindTo(listener)
                    .asType(MethodType.methodType(void.class, RedisEvent.class));

            return new ClassData(method, methodHandle, redisEventClass.asSubclass(RedisEvent.class));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to retrieve class data for RedisEventInvoker", e);
        }
    }
}
