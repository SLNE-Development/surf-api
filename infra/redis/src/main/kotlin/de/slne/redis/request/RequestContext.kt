package de.slne.redis.request

import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Context provided to request handlers that allows sending responses.
 * This allows handlers to respond synchronously or asynchronously.
 */
class RequestContext<TRequest : RedisRequest> internal constructor(
    val request: TRequest,
    val coroutineScope: CoroutineScope,
    private val respondCallback: suspend (RedisResponse) -> Unit
) {
    private val responded = AtomicBoolean(false)
    
    /**
     * Send a response to this request.
     * Can be called from a regular function or from within a coroutine.
     * @param response The response to send
     * @throws IllegalStateException if a response was already sent
     */
    suspend fun respond(response: RedisResponse) {
        if (!responded.compareAndSet(false, true)) {
            throw IllegalStateException("Response already sent for this request")
        }
        respondCallback(response)
    }
}
