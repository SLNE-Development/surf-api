package dev.slne.surf.redis.request

import dev.slne.surf.redis.util.InternalRedisAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Context object provided to Redis request handler methods.
 *
 * A [RequestContext] represents a single incoming [RedisRequest] and exposes:
 * - the request payload via [request]
 * - a controlled mechanism to publish **at most one** [RedisResponse]
 *
 * Since requests are broadcasted, multiple handlers on different servers may
 * respond to the same request. This context enforces the rule that a single
 * handler instance may only send **one** response.
 *
 * `RequestContext` implements [CoroutineScope], bound to the Redis listener scope.
 * Handlers that require asynchronous processing can launch coroutines directly
 * via `launch { }` without managing their own scope.
 *
 * ## Response handling
 * - [respond] may be called synchronously or from within a launched coroutine
 * - Calling [respond] more than once results in an exception
 *
 * @param TRequest the concrete request type handled by this context
 */
class RequestContext<TRequest : RedisRequest> @InternalRedisAPI constructor(
    val request: TRequest,
    private val respondCallback: (RedisResponse) -> Deferred<Long>,
    coroutineScope: CoroutineScope
) : CoroutineScope by coroutineScope {
    private val responded = AtomicBoolean(false)

    /**
     * Publishes a response for this request.
     *
     * Exactly one response is allowed per [RequestContext] instance.
     * This method may be called from a regular function or from within a coroutine.
     *
     * @param response the response to send
     * @return a [Deferred] completing with the number of receiving subscribers
     * @throws IllegalStateException if a response has already been sent
     */
    fun respond(response: RedisResponse): Deferred<Long> {
        if (!responded.compareAndSet(false, true)) {
            throw IllegalStateException("Response already sent for this request")
        }
        return respondCallback(response)
    }

    /**
     * Indicates whether the underlying request originated from this client.
     *
     * @see RedisRequest.originatesFromThisClient
     */
    fun originatesFromThisClient() = request.originatesFromThisClient()
}
