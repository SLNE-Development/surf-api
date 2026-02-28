package dev.slne.surf.redis.request;

import dev.slne.surf.redis.util.RedisHiddenInvokerUtil;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Objects;

@NullMarked
final class RedisRequestHandlerInvokerFactory {

    private static final byte[] TEMPLATE_CLASS_BYTES;
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    static {
        try (final InputStream is = RedisRequestHandlerInvokerTemplate.class.getResourceAsStream(RedisRequestHandlerInvokerTemplate.class.getSimpleName() + ".class")) {
            Objects.requireNonNull(is, "RedisRequestHandlerInvokerTemplate.class not found");
            TEMPLATE_CLASS_BYTES = is.readAllBytes();
        } catch (IOException e) {
            throw new AssertionError("Failed to load RedisRequestHandlerInvokerTemplate.class", e);
        }
    }

    private RedisRequestHandlerInvokerFactory() {
        throw new UnsupportedOperationException("RedisRequestHandlerInvokerFactor is a utility class and cannot be instantiated");
    }

    public static RedisRequestHandlerInvoker create(final Object handler, final Method method, final Class<? extends RedisRequest> requestEventClass) {
        try {
            return RedisHiddenInvokerUtil.createInvoker(LOOKUP, TEMPLATE_CLASS_BYTES, RedisRequestHandlerInvoker.class, handler, method, requestEventClass);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to create RedisRequestHandlerInvoker for " + method, e);
        }
    }

    static RedisHiddenInvokerUtil.ClassData<RedisRequest> classData(final MethodHandles.Lookup lookup) {
        try {
            return RedisHiddenInvokerUtil.loadClassData(lookup, MethodType.methodType(void.class, RequestContext.class), RedisRequest.class);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to retrieve class data for RedisRequestHandlerInvoker", e);
        }
    }
}
