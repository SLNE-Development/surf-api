package dev.slne.surf.redis.request

import dev.slne.surf.redis.request.RequestResponseBus.Companion.DEFAULT_TIMEOUT_MS
import dev.slne.surf.redis.util.Initializable
import dev.slne.surf.redis.util.InternalRedisAPI
import java.io.Closeable

/**
 * Redis-backed request/response bus implemented on top of Redis Pub/Sub.
 *
 * Requests are **broadcasted** to all subscribers. Therefore, multiple servers/handlers may respond
 * to the same request. On the caller side, the request completes with the **first response**
 * that arrives; additional responses may be ignored.
 *
 * Request handlers are discovered via [HandleRedisRequest].
 *
 * ## Threading
 * Handler methods are invoked synchronously on a Redisson/Reactor thread.
 * If suspending or long-running work is required, the handler must explicitly delegate it
 * (for example by launching a coroutine) and call [RequestContext.respond] later.
 *
 * ## Lifecycle
 * The owning [dev.slne.surf.redis.RedisApi] initializes the bus during startup via [init]
 * and closes it during shutdown.
 */
interface RequestResponseBus : Closeable, Initializable {
    companion object {
        /**
         * Default timeout (in milliseconds) for awaiting the first response.
         */
        const val DEFAULT_TIMEOUT_MS = 5000L
    }

    /**
     * Sends [request] and awaits the first matching response of type [responseType].
     *
     * Because requests are broadcasted, more than one server may respond. This method returns
     * the first response that arrives within [timeoutMs].
     *
     * @param request Request payload to publish.
     * @param responseType Expected response type.
     * @param timeoutMs Timeout (milliseconds) to await the first response.
     *
     * @return The first received response.
     *
     * @throws RequestTimeoutException if no response is received within [timeoutMs].
     * @throws ClassCastException if the received response is not an instance of [responseType].
     */
    @Throws(RequestTimeoutException::class)
    suspend fun <T : RedisResponse> sendRequest(
        request: RedisRequest,
        responseType: Class<T>,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): T

    /**
     * Registers request handler methods on the given [handler] instance.
     *
     * The handler object is scanned for methods annotated with [HandleRedisRequest].
     * Method requirements are defined by [HandleRedisRequest].
     *
     * Implementations may restrict duplicate handlers for the same request type.
     *
     * @param handler Object containing [HandleRedisRequest]-annotated methods.
     */
    fun registerRequestHandler(handler: Any)
}

/**
 * Sends a request and awaits a response of type [T].
 *
 * The response type is registered for deserialization using the fully-qualified class name.
 *
 * @param request request payload to publish
 * @param timeoutMs timeout for awaiting the response
 * @return the response payload
 * @throws RequestTimeoutException if no response is received within [timeoutMs]
 * @throws ClassCastException if the received response does not match [T]
 */
@Throws(RequestTimeoutException::class)
suspend inline fun <reified T : RedisResponse> RequestResponseBus.sendRequest(
    request: RedisRequest,
    timeoutMs: Long = DEFAULT_TIMEOUT_MS
): T {
    return sendRequest(request, T::class.java, timeoutMs)
}