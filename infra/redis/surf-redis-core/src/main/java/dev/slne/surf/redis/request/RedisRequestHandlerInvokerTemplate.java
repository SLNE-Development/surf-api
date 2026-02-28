package dev.slne.surf.redis.request;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

final class RedisRequestHandlerInvokerTemplate implements RedisRequestHandlerInvoker {
    private static final Method METHOD;
    private static final MethodHandle HANDLE;
    private static final Class<? extends RedisRequest> REDIS_REQUEST_CLASS;

    static {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        final RedisRequestHandlerInvokerFactory.ClassData classData = RedisRequestHandlerInvokerFactory.classData(lookup);

        METHOD = classData.method();
        HANDLE = classData.methodHandle();
        REDIS_REQUEST_CLASS = classData.requestEventClass();
    }

    @Override
    public void invoke(@NotNull RequestContext<?> context) {
        if (!REDIS_REQUEST_CLASS.isInstance(context.getRequest())) return;
        try {
            HANDLE.invokeExact(context);
        } catch (Throwable t) {
            sneakyThrow(t);
        }
    }

    @Override
    public String toString() {
        return "RedisRequestHandlerInvokerTemplate{" + METHOD + "}";
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(final Throwable t) throws T {
        throw (T) t;
    }
}
