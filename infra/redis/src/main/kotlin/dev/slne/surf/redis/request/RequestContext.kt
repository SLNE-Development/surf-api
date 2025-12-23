package dev.slne.surf.redis.request

import kotlinx.coroutines.Deferred
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Context object provided to request handler methods.
 *
 * A [RequestContext] represents a single incoming request and provides
 * access to both the request payload and a mechanism to send exactly one response.
 *
 * Responses may be sent synchronously or asynchronously.
 * If asynchronous processing is required, the handler must explicitly
 * launch its own coroutine.
 *
 * @param TRequest the concrete request type
 */
class RequestContext<TRequest : RedisRequest> internal constructor(
    val request: TRequest,
    private val respondCallback: (RedisResponse) -> Deferred<Long>
) {
    private val responded = AtomicBoolean(false)

    /**
     * Sends a response for this request.
     *
     * This method may be called from a regular function or from within a coroutine.
     * Exactly one response is allowed per request.
     *
     * @param response the response to send
     * @return a [Deferred] containing the number of receiving subscribers
     * @throws IllegalStateException if a response has already been sent
     */
    fun respond(response: RedisResponse): Deferred<Long> {
        if (!responded.compareAndSet(false, true)) {
            throw IllegalStateException("Response already sent for this request")
        }
        return respondCallback(response)
    }
}
