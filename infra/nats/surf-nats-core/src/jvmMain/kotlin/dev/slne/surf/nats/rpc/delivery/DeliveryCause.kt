package dev.slne.surf.nats.rpc.delivery

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.rpc.NatsRedeliveryException
import io.nats.client.support.NatsJetStreamClientError
import kotlin.coroutines.cancellation.CancellationException

/**
 * Why an attempt to handle a persistent delivery ended.
 *
 * A [permanent] cause repeats on every delivery of the same message, so a fire with a permanent cause is
 * dead-lettered without further redeliveries. [DeliveryDisposition.of] maps a cause to the action on the delivery.
 */
@InternalNatsApi
internal enum class DeliveryCause(val permanent: Boolean = false) {

    /**
     * The attempt produced a reply its flow accepts: an empty reply for a fire, any reply for a persistent RPC.
     */
    COMPLETED,

    /**
     * The attempt failed in a way a later delivery may not.
     */
    FAILED,

    /**
     * The call threw [NatsRedeliveryException].
     */
    REDELIVERY_REQUESTED,

    /**
     * The handler exceeded its time limit and was cancelled.
     */
    TIMED_OUT,

    /**
     * The delivery got no handler slot before its deadline.
     */
    NOT_ADMITTED,

    /**
     * The registration started draining while the delivery waited for a handler slot.
     */
    DRAINING,

    /**
     * The deadline of the call had passed when the delivery arrived.
     */
    EXPIRED(permanent = true),

    /**
     * The offloaded payload no longer exists in the object store.
     */
    PAYLOAD_MISSING(permanent = true),

    /**
     * The request could not be decoded.
     */
    UNDECODABLE(permanent = true),

    /**
     * The interceptor answered a fire with a stored error reply.
     */
    STORED_ERROR(permanent = true),

    /**
     * The reply does not fit the result of the method.
     */
    REPLY_MISMATCH(permanent = true);

    companion object {

        /**
         * Classifies a failure to load the request payload. Only a missing object is permanent. Any other load
         * failure, such as a slow read, may not repeat.
         */
        fun ofLoadFailure(e: Throwable): DeliveryCause = if (isObjectNotFound(e)) PAYLOAD_MISSING else FAILED

        /**
         * Classifies a failure to decode the request. A cancellation says nothing about the request and stays
         * transient.
         */
        fun ofDecodeFailure(e: Throwable): DeliveryCause = if (e is CancellationException) FAILED else UNDECODABLE

        /**
         * Classifies a failure of the handler or interceptor, or of a call whose failing step is unknown.
         */
        fun ofFailure(e: Throwable): DeliveryCause =
            if (e is NatsRedeliveryException) REDELIVERY_REQUESTED else FAILED

        private fun isObjectNotFound(e: Throwable): Boolean {
            return e is IllegalArgumentException &&
                    e.message?.startsWith("[${NatsJetStreamClientError.OsObjectNotFound.id()}]") == true
        }
    }
}
