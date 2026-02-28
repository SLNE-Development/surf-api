package dev.slne.surf.redis.request

/**
 * Functional interface for invoking a registered Redis request handler.
 *
 * Implementations are generated at runtime as **JVM hidden classes**
 * (via [java.lang.invoke.MethodHandles.Lookup.defineHiddenClass]).
 * This approach wraps the underlying [java.lang.invoke.MethodHandle] into a concrete
 * class that the JIT compiler can inline and constant-fold, providing significantly
 * better dispatch performance compared to raw `MethodHandle.invoke()` calls.
 *
 * Each [RedisRequestHandlerInvoker] instance is bound to a specific handler object and method
 * at creation time by [dev.slne.surf.redis.invoker.RedisRequestHandlerInvokerFactory].
 *
 * @see dev.slne.surf.redis.invoker.RedisRequestHandlerInvokerFactory
 * @see RequestResponseBusImpl
 */
fun interface RedisRequestHandlerInvoker {

    /**
     * Dispatches the given request [context] to the bound handler method.
     *
     * This method is called synchronously on a Redisson/Reactor thread during request handling.
     * Exceptions thrown by the underlying handler are propagated to the caller.
     *
     * @param context the request context containing the deserialized request and response callback
     */
    fun invoke(context: RequestContext<*>)
}