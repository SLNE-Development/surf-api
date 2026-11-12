package dev.slne.surf.nats.rpc

/**
 * Wraps the handling of every persistent method of one service registration.
 *
 * Pass it to [dev.slne.surf.nats.NatsClient.registerService]. It runs for persistent fire calls and persistent RPC
 * after the request is decoded, inside the handler's time limit and coroutine. Methods that are not persistent do
 * not run through it. Calling [NatsPersistentCall.proceed] runs the handler in the calling coroutine, so context
 * elements the interceptor installs, such as a database transaction, are visible to the handler. Returning a reply
 * without calling `proceed` skips the handler and answers with that reply, which lets an interceptor answer a
 * redelivered or repeated message with the reply stored for its first execution.
 *
 * The following interceptor stores the reply of each message and answers repeats with it. `ReplyStore` stands for
 * the application's storage. It is keyed by service, method and message ID, because calls to different methods
 * inside one [withNatsIdempotencyKey] block share the same [NatsPersistentCall.messageId]:
 *
 * ```kotlin
 * class StoredReplies(private val store: ReplyStore) : NatsPersistentInterceptor {
 *     override suspend fun intercept(call: NatsPersistentCall): NatsPersistentReply {
 *         store.find(call.serviceName, call.methodName, call.messageId)?.let { (format, bytes) ->
 *             return NatsPersistentReply.restore(format, bytes)
 *         }
 *         return call.proceed().also { reply ->
 *             store.save(call.serviceName, call.methodName, call.messageId, reply.format, reply.bytes)
 *         }
 *     }
 * }
 *
 * client.registerService<OrderService>(OrderServiceImpl(), StoredReplies(store))
 * ```
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
     *
     * @return the reply from [NatsPersistentCall.proceed], or a reply created with [NatsPersistentReply.restore] or
     * [NatsPersistentReply.error] without running the handler.
     * @throws NatsRedeliveryException to request another delivery, as described for this interface.
     */
    public suspend fun intercept(call: NatsPersistentCall): NatsPersistentReply
}
