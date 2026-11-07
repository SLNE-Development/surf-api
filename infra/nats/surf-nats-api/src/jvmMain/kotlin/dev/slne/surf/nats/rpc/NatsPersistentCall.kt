package dev.slne.surf.nats.rpc

import org.jetbrains.annotations.ApiStatus

/**
 * One delivery of a persistent message as seen by a [NatsPersistentInterceptor].
 *
 * [serviceName] and [methodName] identify the method by its proto names and stay stable across releases of the
 * service, unlike Kotlin names. [messageId] and [deliveryCount] match the [NatsPersistentContext] the handler sees.
 *
 * @property serviceName the proto full name of the service, for example `surf.punish.v1.PunishService`.
 * @property methodName the rpc name as declared in the `.proto`.
 * @property messageId the message identity, stable across redeliveries and dead-letter replays of the same message.
 * A caller that retries after a timeout sends a new message with a new identity.
 * @property deliveryCount the number of times the message has been delivered, starting at one.
 * @property requestFingerprint the lowercase hexadecimal SHA-256 of the request bytes as received, before decoding.
 * Redeliveries carry the same bytes; requests encoded separately from equal values may differ.
 */
@ApiStatus.NonExtendable
public interface NatsPersistentCall {
    public val serviceName: String
    public val methodName: String
    public val messageId: String
    public val deliveryCount: Long
    public val requestFingerprint: String

    /**
     * Runs the handler in the calling coroutine and returns its encoded reply.
     *
     * The response is encoded before this function returns, so it can be stored in the same transaction as the
     * handler's effects. A fire call returns an empty reply. Exceptions of the handler and of the encoding propagate
     * unchanged.
     *
     * @throws IllegalStateException if `proceed` was already called for this delivery.
     */
    public suspend fun proceed(): NatsPersistentReply
}
