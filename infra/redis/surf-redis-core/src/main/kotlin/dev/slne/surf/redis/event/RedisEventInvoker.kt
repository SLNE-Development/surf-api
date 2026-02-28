package dev.slne.surf.redis.event

/**
 * Functional interface for invoking a registered Redis event handler.
 *
 * Implementations are generated at runtime as **JVM hidden classes**
 * (via [java.lang.invoke.MethodHandles.Lookup.defineHiddenClass]).
 * This approach wraps the underlying [java.lang.invoke.MethodHandle] into a concrete
 * class that the JIT compiler can inline and constant-fold, providing significantly
 * better dispatch performance compared to raw `MethodHandle.invoke()` calls.
 *
 * Each [RedisEventInvoker] instance is bound to a specific listener object and handler method
 * at creation time by [dev.slne.surf.redis.invoker.RedisEventInvokerFactory].
 *
 * @see dev.slne.surf.redis.invoker.RedisEventInvokerFactory
 * @see RedisEventBusImpl
 */
fun interface RedisEventInvoker {
    /**
     * Dispatches the given [event] to the bound handler method.
     *
     * This method is called synchronously on a Redisson/Reactor thread during event delivery.
     * Exceptions thrown by the underlying handler are propagated to the caller.
     *
     * @param event the deserialized event to dispatch to the handler
     */
    fun invoke(event: RedisEvent)
}