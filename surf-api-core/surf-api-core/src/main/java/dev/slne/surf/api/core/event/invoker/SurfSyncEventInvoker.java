package dev.slne.surf.api.core.event.invoker;

import dev.slne.surf.api.core.event.SurfSyncEvent;
import dev.slne.surf.api.shared.api.util.InternalInvokerApi;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

/**
 * Functional interface for invoking a single sync event handler via a hidden-class-backed
 * method handle.
 */
@FunctionalInterface
@InternalInvokerApi
@ApiStatus.Internal
@NullMarked
public interface SurfSyncEventInvoker {
    void invoke(SurfSyncEvent event);
}
