package dev.slne.surf.redis.invoker;

import dev.slne.surf.api.core.invoker.HiddenInvokerUtil;
import dev.slne.surf.api.core.invoker.InvokerClassData;
import dev.slne.surf.redis.request.RedisRequestHandlerInvoker;
import dev.slne.surf.redis.request.RequestContext;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Hidden class template for Redis request handler invocation.
 */
@SuppressWarnings("UnstableApiUsage")
public final class RedisRequestHandlerInvokerTemplate implements RedisRequestHandlerInvoker {

    private static final Method METHOD;
    private static final MethodHandle HANDLE;
    private static final Class<?> REDIS_REQUEST_CLASS;
    private static final boolean IS_SUSPEND;

    static {
        try {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            final InvokerClassData classData = HiddenInvokerUtil.loadClassDataWithAutoSuspend(
                lookup, MethodType.methodType(void.class, RequestContext.class));

            METHOD = classData.method();
            HANDLE = classData.methodHandle();
            REDIS_REQUEST_CLASS = classData.payloadClass();
            IS_SUSPEND = classData.isSuspend();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to initialize RedisRequestHandlerInvokerTemplate", e);
        }
    }

    @Override
    public @Nullable Object invoke(@NotNull RequestContext<?> context,
        @NotNull Continuation<? super @NotNull Unit> $completion) {
        if (!REDIS_REQUEST_CLASS.isInstance(context.getRequest())) {
            return Unit.INSTANCE;
        }

        if (IS_SUSPEND) {
            try {
                HANDLE.invoke(context, $completion);
            } catch (Throwable t) {
                HiddenInvokerUtil.sneakyThrow(t);
            }
        } else {
            try {
                HANDLE.invokeExact(context);
            } catch (Throwable t) {
                HiddenInvokerUtil.sneakyThrow(t);
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return "RedisRequestHandlerInvokerTemplate{" + METHOD + "}";
    }
}
