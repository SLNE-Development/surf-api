package dev.slne.surf.redis.event;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

/**
 * Hidden class template for Redis event handler invocation.
 *
 * <p>This class is designed to be used with
 * {@link MethodHandles.Lookup#defineHiddenClassWithClassData(byte[], Object, boolean, MethodHandles.Lookup.ClassOption...)}.
 * Initializing it directly will fail due to missing {@code classData}.
 *
 * <p>The {@code classData} must be a {@link java.util.List} containing:
 * <ol>
 *     <li>The listener instance ({@link Object})</li>
 *     <li>The Redis event class ({@link Class})</li>
 *     <li>The handler {@link Method}</li>
 *     <li>A {@link MethodHandles.Lookup} with access to the listener's class (via {@code privateLookupIn})</li>
 * </ol>
 *
 * <p>The MethodHandle is resolved in the static initializer and stored as a
 * {@code static final} field, allowing the JIT compiler to treat it as a
 * compile-time constant and fully inline the call.
 */
final class RedisEventInvokerTemplate implements RedisEventInvoker {
    private static final Method METHOD;
    private static final MethodHandle HANDLE;
    private static final Class<? extends RedisEvent> REDIS_EVENT_CLASS;

    static {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        final RedisEventInvokerFactory.ClassData classData = RedisEventInvokerFactory.classData(lookup);

        METHOD = classData.method();
        HANDLE = classData.methodHandle();
        REDIS_EVENT_CLASS = classData.redisEventClass();
    }


    @Override
    public void invoke(@NotNull RedisEvent event) {
        if (!REDIS_EVENT_CLASS.isInstance(event)) return;
        try {
            HANDLE.invokeExact(event);
        } catch (Throwable t) {
            sneakyThrow(t);
        }
    }

    @Override
    public String toString() {
        return "RedisEventInvokerTemplate{" + METHOD + "}";
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(final Throwable t) throws T {
        throw (T) t;
    }
}
