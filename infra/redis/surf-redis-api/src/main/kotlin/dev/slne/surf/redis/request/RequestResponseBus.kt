package dev.slne.surf.redis.request

import dev.slne.surf.redis.request.RequestResponseBus.Companion.DEFAULT_TIMEOUT_MS
import dev.slne.surf.redis.util.InternalRedisAPI
import java.io.Closeable

/**
 * Redis-backed request/response bus based on Redis Pub/Sub.
 *
 * This bus supports:
 * - Publishing requests to the request channel
 * - Handling incoming requests via methods annotated with [HandleRedisRequest]
 * - Publishing responses to the response channel
 * - Awaiting responses for outgoing requests using a per-request correlation ID
 *
 * Handler methods are invoked synchronously on a Redisson/Reactor thread.
 * If suspending or long-running work is required, the handler must launch its own coroutine.
 */
interface RequestResponseBus : Closeable {
    companion object {
        /**
         * Default timeout for awaiting responses in milliseconds.
         */
        const val DEFAULT_TIMEOUT_MS = 5000L
    }

    /**
     * Initializes the bus by subscribing to request/response channels.
     *
     * This method should only be called during startup.
     */
    @InternalRedisAPI
    fun init()

    /**
     * Sends a request and awaits a response of the given [responseType].
     *
     * @param request request payload to publish
     * @param responseType expected response type
     * @param timeoutMs timeout for awaiting the response
     * @return the response payload
     * @throws RequestTimeoutException if no response is received within [timeoutMs]
     * @throws ClassCastException if the received response does not match [responseType]
     */
    suspend fun <T : RedisResponse> sendRequest(
        request: RedisRequest,
        responseType: Class<T>,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): T

    /**
     * Registers all request handler methods on the given object.
     *
     * Methods annotated with [HandleRedisRequest] must:
     * - Have exactly one parameter
     * - The parameter type must be `RequestContext<T>`
     * - `T` must be a subtype of [RedisRequest]
     * - Not be a `suspend` function
     *
     * Only a single handler may be registered per request type; duplicates are ignored.
     *
     * @param handler object containing [HandleRedisRequest]-annotated methods
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
suspend inline fun <reified T : RedisResponse> RequestResponseBus.sendRequest(
    request: RedisRequest,
    timeoutMs: Long = DEFAULT_TIMEOUT_MS
): T {
    return sendRequest(request, T::class.java, timeoutMs)
}