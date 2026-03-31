package dev.slne.surf.redis.invoker;

import dev.slne.surf.redis.event.RedisEvent;
import dev.slne.surf.redis.event.RedisEventInvoker;
import dev.slne.surf.surfapi.core.api.invoker.HiddenInvokerUtil;
import dev.slne.surf.surfapi.core.api.invoker.InvokerClassData;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

/**
 * Hidden class template for Redis event invocation.
 */
@SuppressWarnings("UnstableApiUsage")
public final class RedisEventInvokerTemplate implements RedisEventInvoker {
    private static final Method METHOD;
    private static final MethodHandle HANDLE;
    private static final Class<?> REDIS_EVENT_CLASS;
    private static final boolean IS_SUSPEND;

    static {
        try {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            final InvokerClassData classData = HiddenInvokerUtil.loadClassDataWithAutoSuspend(lookup, MethodType.methodType(void.class, RedisEvent.class));

            METHOD = classData.method();
            HANDLE = classData.methodHandle();
            REDIS_EVENT_CLASS = classData.payloadClass();
            IS_SUSPEND = classData.isSuspend();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to initialize RedisEventInvokerTemplate", e);
        }
    }

    @Override
    public @Nullable Object invoke(@NotNull RedisEvent event, @NotNull Continuation<? super @NotNull Unit> $completion) {
        if (!REDIS_EVENT_CLASS.isInstance(event)) return Unit.INSTANCE;

        if (IS_SUSPEND) {
            try {
                return HANDLE.invoke(event, $completion);
            } catch (Throwable t) {
                HiddenInvokerUtil.sneakyThrow(t);
            }
        } else {
            try {
                HANDLE.invokeExact(event);
            } catch (Throwable t) {
                HiddenInvokerUtil.sneakyThrow(t);
            }
            return Unit.INSTANCE;
        }

        return null;
    }

    @Override
    public String toString() {
        return "RedisEventInvokerTemplate{" + METHOD + "}";
    }
}
