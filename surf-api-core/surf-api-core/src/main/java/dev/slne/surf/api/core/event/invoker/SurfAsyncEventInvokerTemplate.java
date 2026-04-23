package dev.slne.surf.api.core.event.invoker;

import dev.slne.surf.api.core.event.SurfAsyncEvent;
import dev.slne.surf.api.core.invoker.HiddenInvokerUtil;
import dev.slne.surf.api.core.invoker.InvokerClassData;
import dev.slne.surf.api.shared.api.util.InternalInvokerApi;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

/**
 * Template class whose bytecode is loaded as a JVM hidden class to implement
 * {@link SurfAsyncEventInvoker} for a single handler method.
 *
 * <p>This template supports both regular and Kotlin {@code suspend} handler
 * methods – {@link HiddenInvokerUtil#loadClassDataWithAutoSuspend} configures
 * the bound {@link java.lang.invoke.MethodHandle} accordingly.
 *
 * <p>The template is never instantiated directly – see
 * {@link SurfSyncEventInvokerTemplate} for details on the hidden-class
 * lifecycle.
 */
@NullMarked
@InternalInvokerApi
@ApiStatus.Internal
public final class SurfAsyncEventInvokerTemplate implements SurfAsyncEventInvoker {

    private static final InvokerClassData DATA;

    static {
        try {
            DATA = HiddenInvokerUtil.loadClassDataWithAutoSuspend(
                MethodHandles.lookup(),
                MethodType.methodType(void.class, SurfAsyncEvent.class)
            );
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Public no-args constructor required by
     * {@link java.lang.invoke.MethodHandles.Lookup#findConstructor}.
     */
    public SurfAsyncEventInvokerTemplate() {
    }

    @Override
    public Object invoke(final SurfAsyncEvent event, final Continuation<?> continuation) {
        try {
            if (DATA.isSuspend()) {
                return DATA.methodHandle().invoke(event, continuation);
            } else {
                DATA.methodHandle().invoke(event);
                return Unit.INSTANCE;
            }
        } catch (Throwable t) {
            HiddenInvokerUtil.sneakyThrow(t);
            return null; // unreachable – sneakyThrow always throws
        }
    }
}
