package dev.slne.surf.redis.event;

import dev.slne.surf.redis.util.RedisHiddenInvokerUtil;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
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
            return RedisHiddenInvokerUtil.createInvoker(LOOKUP, TEMPLATE_CLASS_BYTES, RedisEventInvoker.class, listener, method, redisEventClass);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to create RedisEventInvoker for " + method, e);
        }
    }

    static RedisHiddenInvokerUtil.ClassData<RedisEvent> classData(MethodHandles.Lookup lookup) {
        try {
            return RedisHiddenInvokerUtil.loadClassData(lookup, MethodType.methodType(void.class, RedisEvent.class), RedisEvent.class);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to retrieve class data for RedisEventInvoker", e);
        }
    }
}
