package dev.slne.surf.api.core.event.invoker;

import dev.slne.surf.api.core.event.SurfAsyncEvent;
import dev.slne.surf.api.shared.api.util.InternalInvokerApi;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

/**
 * Hidden-class-backed invoker for a single {@link SurfAsyncEvent} handler
 * method, supporting both regular and Kotlin {@code suspend} handlers.
 *
 * <p>The handler method's compiled signature determines what the invoker
 * does:
 * <ul>
 *   <li>For a non-suspend handler the invoker calls it synchronously and
 *       returns {@link kotlin.Unit#INSTANCE}.</li>
 *   <li>For a {@code suspend} handler the invoker forwards the
 *       {@link Continuation} to the handler, returning whatever the handler
 *       returns (typically {@link kotlin.coroutines.intrinsics.IntrinsicsKt#COROUTINE_SUSPENDED}
 *       when the coroutine actually suspends).</li>
 * </ul>
 *
 * <p>Implementations are generated at runtime from
 * {@link SurfAsyncEventInvokerTemplate}.
 */
@FunctionalInterface
@NullMarked
@InternalInvokerApi
@ApiStatus.Internal
public interface SurfAsyncEventInvoker {

    /**
     * Invokes the bound handler with the given event and Kotlin continuation.
     *
     * @param event        the event being dispatched
     * @param continuation the Kotlin continuation, used only when the handler
     *                     is a suspend function
     * @return either the handler's result, {@code Unit.INSTANCE} for a
     * non-suspend handler, or {@code COROUTINE_SUSPENDED} when a suspend
     * handler actually suspended.
     */
    Object invoke(SurfAsyncEvent event, Continuation<?> continuation);
}
