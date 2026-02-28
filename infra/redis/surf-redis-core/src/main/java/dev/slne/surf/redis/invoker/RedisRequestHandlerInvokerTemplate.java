package dev.slne.surf.redis.invoker;

import dev.slne.surf.redis.request.RedisRequest;
import dev.slne.surf.redis.request.RedisRequestHandlerInvoker;
import dev.slne.surf.redis.request.RequestContext;
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
        final RedisHiddenInvokerUtil.ClassData<RedisRequest> classData = RedisRequestHandlerInvokerFactory.classData(lookup);

        METHOD = classData.method();
        HANDLE = classData.methodHandle();
        REDIS_REQUEST_CLASS = classData.payloadClass();
    }

    @Override
    public void invoke(@NotNull RequestContext<?> context) {
        if (!REDIS_REQUEST_CLASS.isInstance(context.getRequest())) return;
        try {
            HANDLE.invokeExact(context);
        } catch (Throwable t) {
            InvokerUtils.sneakyThrow(t);
        }
    }

    @Override
    public String toString() {
        return "RedisRequestHandlerInvokerTemplate{" + METHOD + "}";
    }
}
