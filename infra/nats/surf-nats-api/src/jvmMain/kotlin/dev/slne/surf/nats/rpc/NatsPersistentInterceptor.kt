package dev.slne.surf.nats.rpc

/**
 * Wraps the handling of every persistent method of one service registration.
 *
 * Runs for persistent fire calls and persistent RPC after the request payload is resolved and decoded, inside the
 * handler's timeout and coroutine. Calling [NatsPersistentCall.proceed] runs the handler in the calling coroutine, so
 * context elements the interceptor installs, such as a database transaction, are visible to the handler. Returning a
 * reply without calling `proceed` skips the handler and answers with that reply, which lets an interceptor answer a
 * redelivered or retried message with the reply stored for its first execution.
 *
 * Failure policy:
 * - A [NatsRedeliveryException] asks for the message to be delivered again, as described there.
 * - Any other exception is a final failure: a persistent RPC is answered with an error reply and acknowledged, a fire
 *   call is redelivered with backoff and dead-lettered after the last delivery, as without an interceptor.
 * - A reply in the error format fails a fire call permanently, so it is dead-lettered without redelivery.
 *
 * The same interceptor instance may run concurrently for different messages. A redelivery of a message may overlap
 * its previous delivery when extending the previous delivery's acknowledgement deadline fails.
 */
public fun interface NatsPersistentInterceptor {
    /**
     * Handles [call] and returns the reply to send.
     */
    public suspend fun intercept(call: NatsPersistentCall): NatsPersistentReply
}
