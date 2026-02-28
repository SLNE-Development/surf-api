package dev.slne.surf.redis.event

/**
 * Functional interface for invoking a Redis event handler.
 *
 * Implementations are generated at runtime as hidden classes
 * to allow JIT-constant-folding of the underlying MethodHandle.
 */
fun interface RedisEventInvoker {
    /**
     * Invokes the event handler with the given event.
     *
     * @param event the event to dispatch to the handler
     */
    fun invoke(event: RedisEvent)
}