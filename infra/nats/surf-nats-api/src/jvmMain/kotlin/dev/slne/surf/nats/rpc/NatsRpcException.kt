@file:Suppress("CanBeParameter")

package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import java.io.Serial
import kotlin.time.Duration

/**
 * Represents a failure of a remote service call.
 *
 * Calls through a service proxy throw the subtypes for the outcomes described in [Recipients]. The base type itself
 * is thrown when the server rejects the publish of a persistent call.
 */
public open class NatsRpcException @InternalNatsApi constructor(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    internal companion object {
        @Serial
        private const val serialVersionUID: Long = 2052306227954268506L
    }
}

/**
 * Reports a failure while the remote instance handles a service call or prepares its response.
 *
 * Carries the remote exception's type name and message, without its stack trace or cause. The original exception
 * type is not recreated locally, so return expected domain outcomes in the response message instead of throwing them.
 *
 * @property remoteType the remote exception's binary class name.
 * @property remoteMessage the remote message, truncated to at most 4096 characters, or empty if absent.
 */
public class NatsRemoteException @InternalNatsApi constructor(
    public val remoteType: String,
    public val remoteMessage: String,
) : NatsRpcException(NatsRpcMessages.remote(remoteType, remoteMessage)) {
    internal companion object {
        @Serial
        private const val serialVersionUID: Long = 769895995432718802L
    }
}

/**
 * Indicates that no instance serves a non-persistent [Recipients.ANY_INSTANCE] method.
 *
 * @property subject the subject addressed by the call.
 */
public class NatsNoRespondersException @InternalNatsApi constructor(
    public val subject: String,
) : NatsRpcException(NatsRpcMessages.noResponders(subject)) {
    internal companion object {
        @Serial
        private const val serialVersionUID: Long = 8318956389694705193L
    }
}

/**
 * Indicates that no response or storage confirmation arrived within the call timeout.
 *
 * A fire call throws it when the server has not confirmed the storage in time. Calls that wait for a response,
 * including [Recipients.ANY_INSTANCE] methods returning [Unit], throw it when the response does not arrive in time,
 * also when the handler exceeded the time left until the caller's deadline.
 * Not thrown by [Recipients.ALL_INSTANCES] calls.
 *
 * @property subject the subject addressed by the call.
 * @property timeout the timeout used for the call.
 */
public class NatsCallTimeoutException @InternalNatsApi constructor(
    public val subject: String,
    public val timeout: Duration,
) : NatsRpcException(NatsRpcMessages.callTimeout(subject, timeout)) {
    internal companion object {
        @Serial
        private const val serialVersionUID: Long = 6955693591776044694L
    }
}