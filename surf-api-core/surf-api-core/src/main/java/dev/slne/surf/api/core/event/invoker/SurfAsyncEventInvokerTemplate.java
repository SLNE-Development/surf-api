package dev.slne.surf.api.core.event.invoker;

import dev.slne.surf.api.core.event.SurfAsyncEvent;
import dev.slne.surf.api.core.invoker.HiddenInvokerUtil;
import dev.slne.surf.api.core.invoker.InvokerClassData;
import dev.slne.surf.api.shared.api.util.InternalInvokerApi;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

/**
 * Hidden-class template for asynchronous (optionally suspend) event handler invocation.
 *
 * <p>Instances of this class are never created directly; they are defined as JVM hidden classes
 * via {@link HiddenInvokerUtil#createInvoker} and bound to a specific target and method.
 */
@InternalInvokerApi
@ApiStatus.Internal
@NullMarked
public class SurfAsyncEventInvokerTemplate implements SurfAsyncEventInvoker {

    private static final InvokerClassData classData;

    static {
        try {
            classData = HiddenInvokerUtil.loadClassDataWithAutoSuspend(
                MethodHandles.lookup(),
                MethodType.methodType(void.class, SurfAsyncEvent.class)
            );
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public Object invoke(final SurfAsyncEvent event, final Continuation<?> continuation) {
        try {
            if (!classData.payloadClass().isInstance(event)) return null;
            if (classData.isSuspend()) {
                return classData.methodHandle().invoke(event, continuation);
            } else {
                classData.methodHandle().invoke(event);
                return null;
            }
        } catch (Throwable t) {
            HiddenInvokerUtil.sneakyThrow(t);
            return null;
        }
    }
}
