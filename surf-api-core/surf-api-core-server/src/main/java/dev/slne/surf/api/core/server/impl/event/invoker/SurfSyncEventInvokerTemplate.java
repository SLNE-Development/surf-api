package dev.slne.surf.api.core.server.impl.event.invoker;

import dev.slne.surf.api.core.event.SurfSyncEvent;
import dev.slne.surf.api.core.invoker.HiddenInvokerUtil;
import dev.slne.surf.api.core.invoker.InvokerClassData;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

final class SurfSyncEventInvokerTemplate implements SurfSyncEventInvoker {
    private static final Method METHOD;
    private static final MethodHandle HANDLE;
    private static final Class<?> EVENT_TYPE;

    static {
        try {
            final InvokerClassData data = HiddenInvokerUtil.loadClassData(
                    MethodHandles.lookup(),
                    MethodType.methodType(void.class, SurfSyncEvent.class)
            );
            METHOD = data.method();
            HANDLE = data.methodHandle();
            EVENT_TYPE = data.payloadClass();
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public void invoke(@NotNull SurfSyncEvent event) {
        if (!EVENT_TYPE.isInstance(event)) return;
        try {
            HANDLE.invokeExact(event);
        } catch (Throwable e) {
            HiddenInvokerUtil.sneakyThrow(e);
        }
    }

    @Override
    public String toString() {
        return "SurfSyncEventInvokerTemplate[" + METHOD + "]";
    }
}
