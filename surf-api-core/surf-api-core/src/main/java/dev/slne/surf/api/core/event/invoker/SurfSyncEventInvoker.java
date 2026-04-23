package dev.slne.surf.api.core.event.invoker;

import dev.slne.surf.api.core.event.SurfSyncEvent;
import dev.slne.surf.api.shared.api.util.InternalInvokerApi;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

/**
 * Hidden-class-backed invoker for a single {@link SurfSyncEvent} handler
 * method.
 *
 * <p>One instance is created per registered handler. Implementations are
 * generated at runtime from {@link SurfSyncEventInvokerTemplate} and bound to
 * the listener instance and handler method.
 *
 * <p>This interface is part of the internal invoker infrastructure shared by
 * surf-* plugins and must not be used directly.
 */
@FunctionalInterface
@NullMarked
@InternalInvokerApi
@ApiStatus.Internal
public interface SurfSyncEventInvoker {

    /**
     * Invokes the bound handler with the given event.
     *
     * @param event the event being dispatched
     */
    void invoke(SurfSyncEvent event);
}
