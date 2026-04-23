package dev.slne.surf.api.core.event.invoker;

import dev.slne.surf.api.core.event.SurfSyncEvent;
import dev.slne.surf.api.core.invoker.HiddenInvokerUtil;
import dev.slne.surf.api.core.invoker.InvokerClassData;
import dev.slne.surf.api.shared.api.util.InternalInvokerApi;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

/**
 * Hidden-class template for synchronous event handler invocation.
 *
 * <p>Instances of this class are never created directly; they are defined as JVM hidden classes
 * via {@link HiddenInvokerUtil#createInvoker} and bound to a specific target and method.
 */
@InternalInvokerApi
@ApiStatus.Internal
@NullMarked
public class SurfSyncEventInvokerTemplate implements SurfSyncEventInvoker {

    private static final InvokerClassData classData;

    static {
        try {
            classData = HiddenInvokerUtil.loadClassData(
                MethodHandles.lookup(),
                MethodType.methodType(void.class, SurfSyncEvent.class)
            );
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public void invoke(final SurfSyncEvent event) {
        try {
            if (!classData.payloadClass().isInstance(event)) return;
            classData.methodHandle().invoke(event);
        } catch (Throwable t) {
            HiddenInvokerUtil.sneakyThrow(t);
        }
    }
}
