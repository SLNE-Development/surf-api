package dev.slne.surf.api.core.event.invoker;

import dev.slne.surf.api.core.event.SurfAsyncEvent;
import dev.slne.surf.api.shared.api.util.InternalInvokerApi;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

/**
 * Functional interface for invoking a single async (potentially suspend) event handler via a
 * hidden-class-backed method handle.
 *
 * <p>The {@code continuation} parameter carries the Kotlin coroutine state.
 * The return value follows the suspend-function contract: either the result value or
 * {@link kotlin.coroutines.intrinsics.IntrinsicsKt#getCOROUTINE_SUSPENDED()}.
 */
@FunctionalInterface
@InternalInvokerApi
@ApiStatus.Internal
@NullMarked
public interface SurfAsyncEventInvoker {
    Object invoke(SurfAsyncEvent event, Continuation<?> continuation);
}
