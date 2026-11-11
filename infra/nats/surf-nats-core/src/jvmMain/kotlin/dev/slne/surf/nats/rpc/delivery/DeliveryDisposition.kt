package dev.slne.surf.nats.rpc.delivery

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.rpc.RpcDefaults
import dev.slne.surf.nats.runtime.rpc.CallFlow
import kotlin.time.Duration

/**
 * What a responder does with a persistent delivery once an attempt to handle it has ended.
 */
@InternalNatsApi
internal sealed interface DeliveryDisposition {

    /**
     * Acknowledge the delivery. A persistent RPC answers its caller first, with the reply or the error.
     */
    data object Ack : DeliveryDisposition

    /**
     * Request a redelivery after [delay].
     */
    data class Nak(val delay: Duration) : DeliveryDisposition

    /**
     * Remove the delivery without another attempt and without a dead letter.
     */
    data object Term : DeliveryDisposition

    /**
     * Publish the delivery to the dead-letter stream and terminate it once the publish is confirmed. An
     * unconfirmed publish requests a redelivery after [retryDelay] instead, so the message is not lost.
     */
    data class DeadLetter(val retryDelay: Duration) : DeliveryDisposition

    companion object {

        /**
         * Decides what happens to delivery number [delivered] of a [flow] call whose attempt ended with [cause].
         *
         * Nobody waits for a fire: a failure is redelivered and ends in the dead-letter stream, at once when
         * [cause] is permanent, otherwise on the last delivery. A fire that got no handler slot is redelivered on
         * every delivery and never dead-lettered. A persistent RPC answers its caller and is acknowledged, unless
         * the caller has given up, or it requested a redelivery that can still happen: another delivery remains
         * and its backoff ends before [timeLeft] has passed.
         *
         * [timeLeft] is the time until the caller of a persistent RPC gives up; a fire does not read it. [backoff]
         * holds the redelivery delay after each delivery; its last entry repeats. Throws [IllegalStateException]
         * for [CallFlow.CORE], which has no deliveries.
         */
        fun of(
            cause: DeliveryCause,
            flow: CallFlow,
            delivered: Long,
            timeLeft: Duration,
            backoff: List<Duration> = RpcDefaults.NAK_BACKOFF,
        ): DeliveryDisposition = when (flow) {
            CallFlow.FIRE -> ofFire(cause, delivered, backoff)
            CallFlow.PERSISTENT_RPC -> ofPersistentRpc(cause, delivered, timeLeft, backoff)
            CallFlow.CORE -> error("A core call has no delivery")
        }

        private fun ofFire(cause: DeliveryCause, delivered: Long, backoff: List<Duration>): DeliveryDisposition {
            return when {
                cause == DeliveryCause.COMPLETED -> Ack
                cause == DeliveryCause.NOT_ADMITTED -> Nak(backoff.after(delivered))
                cause.permanent || isLastDelivery(delivered) -> DeadLetter(retryDelay = backoff.after(delivered))
                else -> Nak(backoff.after(delivered))
            }
        }

        private fun ofPersistentRpc(
            cause: DeliveryCause,
            delivered: Long,
            timeLeft: Duration,
            backoff: List<Duration>,
        ): DeliveryDisposition = when (cause) {
            DeliveryCause.TIMED_OUT, DeliveryCause.NOT_ADMITTED -> Term
            DeliveryCause.REDELIVERY_REQUESTED -> {
                val delay = backoff.after(delivered)
                if (isLastDelivery(delivered) || delay >= timeLeft) Ack else Nak(delay)
            }

            else -> Ack
        }

        private fun isLastDelivery(delivered: Long): Boolean = delivered >= RpcDefaults.MAX_DELIVER

        private fun List<Duration>.after(delivered: Long): Duration =
            this[(delivered - 1).toInt().coerceIn(0, lastIndex)]
    }
}
