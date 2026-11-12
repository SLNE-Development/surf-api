package dev.slne.surf.nats.js.rpc

import dev.slne.surf.nats.js.error.NatsCallTimeoutError
import dev.slne.surf.nats.js.error.NatsNoRespondersError
import dev.slne.surf.nats.js.external.nats.core.NoRespondersError
import dev.slne.surf.nats.js.external.nats.core.RequestError
import dev.slne.surf.nats.js.external.nats.core.TimeoutError

/**
 * Maps a failed nats.js request to the surf-nats error, or returns [error] unchanged.
 */
internal fun mapRequestError(error: Throwable, subject: String, timeoutMs: Int): Throwable = when {
    isNoResponders(error) -> NatsNoRespondersError(subject)
    isTimeout(error) -> NatsCallTimeoutError(subject, timeoutMs)
    else -> error
}

internal fun isTimeout(error: Throwable): Boolean {
    return error is TimeoutError || (error is RequestError && error.cause is TimeoutError)
}

private fun isNoResponders(error: Throwable): Boolean {
    return error is NoRespondersError || (error is RequestError && error.isNoResponders())
}
