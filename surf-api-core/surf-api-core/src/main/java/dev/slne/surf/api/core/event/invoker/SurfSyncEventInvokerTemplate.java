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
 * Template class whose bytecode is loaded as a JVM hidden class to implement
 * {@link SurfSyncEventInvoker} for a single handler method.
 *
 * <p>The template is never instantiated directly – the
 * {@link dev.slne.surf.api.core.invoker.InvokerFactory} reads its
 * {@code .class} bytes and uses
 * {@link java.lang.invoke.MethodHandles.Lookup#defineHiddenClassWithClassData}
 * to create per-handler subclasses.
 *
 * <p>Sync templates do <b>not</b> support suspend handlers – the registration
 * code in {@code SurfEventBusImpl} validates this before creating the
 * invoker.
 */
@NullMarked
@InternalInvokerApi
@ApiStatus.Internal
public final class SurfSyncEventInvokerTemplate implements SurfSyncEventInvoker {

    private static final InvokerClassData DATA;

    static {
        try {
            DATA = HiddenInvokerUtil.loadClassData(
                MethodHandles.lookup(),
                MethodType.methodType(void.class, SurfSyncEvent.class)
            );
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Public no-args constructor required by
     * {@link java.lang.invoke.MethodHandles.Lookup#findConstructor}.
     */
    public SurfSyncEventInvokerTemplate() {
    }

    @Override
    public void invoke(final SurfSyncEvent event) {
        try {
            DATA.methodHandle().invoke(event);
        } catch (Throwable t) {
            HiddenInvokerUtil.sneakyThrow(t);
        }
    }
}
