package dev.slne.surf.api.core.server.impl.event.invoker;

import dev.slne.surf.api.core.event.SurfAsyncEvent;
import dev.slne.surf.api.core.invoker.HiddenInvokerUtil;
import dev.slne.surf.api.core.invoker.InvokerClassData;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

final class SurfAsyncEventInvokerTemplate implements SurfAsyncEventInvoker {

    private static final Method METHOD;
    private static final MethodHandle HANDLE;
    private static final Class<?> EVENT_TYPE;
    private static final boolean IS_SUSPEND;

    static {
        try {
            final InvokerClassData data = HiddenInvokerUtil.loadClassDataWithAutoSuspend(
                    MethodHandles.lookup(),
                    MethodType.methodType(void.class, SurfAsyncEvent.class)
            );
            METHOD = data.method();
            HANDLE = data.methodHandle();
            EVENT_TYPE = data.payloadClass();
            IS_SUSPEND = data.isSuspend();
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public @Nullable Object invoke(@NotNull SurfAsyncEvent event, @NotNull Continuation<? super @NotNull Unit> $completion) {
        if (!EVENT_TYPE.isInstance(event)) return Unit.INSTANCE;

        if (IS_SUSPEND) {
            try {
                return HANDLE.invoke(event, $completion);
            } catch (Throwable e) {
                HiddenInvokerUtil.sneakyThrow(e);
            }
        } else {
            try {
                HANDLE.invokeExact(event);
            } catch (Throwable e) {
                HiddenInvokerUtil.sneakyThrow(e);
            }
            return Unit.INSTANCE;
        }

        return null;
    }

    @Override
    public String toString() {
        return "SurfAsyncEventInvokerTemplate[" + METHOD + "]";
    }
}
