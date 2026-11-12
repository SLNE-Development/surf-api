package dev.slne.surf.nats.rpc.delivery

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.rpc.RpcDefaults
import dev.slne.surf.nats.runtime.rpc.CallFlow
import kotlin.time.Duration

/**
 * What a responder does with a persistent delivery once an attempt to handle it has ended.
 *
 * Values are pure decisions computed by [of]. The service registration executes them on the JetStream message.
 */
@InternalNatsApi
internal sealed interface DeliveryDisposition {

    /**
     * Acknowledge the delivery. A persistent RPC answers its caller first, with the reply or the error.
     *
     * An offloaded request payload is deleted only after the server confirmed the acknowledgement.
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
     * Publish the delivery to the dead-letter stream and terminate it once the publish is confirmed.
     *
     * An unconfirmed publish requests a redelivery after [retryDelay] instead of terminating. Before the last
     * delivery the message comes back for another attempt. On the last delivery the consumer delivers it no
     * more, and the message stays in the work-queue stream without a dead letter.
     */
    data class DeadLetter(val retryDelay: Duration) : DeliveryDisposition

    companion object {

        /**
         * Decides what happens to delivery number [delivered] of a [flow] call whose attempt ended with [cause].
         *
         * Nobody waits for a fire: a failure is redelivered and ends in the dead-letter stream, at once when
         * [cause] is permanent, otherwise on the last delivery. This includes a fire that got no handler slot or met
         * a draining registration. A persistent RPC answers its caller and is acknowledged, unless the caller has
         * given up, or it requested a redelivery that can still happen: another delivery remains and its backoff
         * ends before [timeLeft] has passed. A persistent RPC that met a draining registration is redelivered after
         * [RpcDefaults.DRAIN_REDELIVERY_DELAY] under the same condition and terminated otherwise.
         *
         * [timeLeft] is the time until the caller of a persistent RPC gives up. A fire does not read it. [backoff]
         * holds the redelivery delay after each delivery, and its last entry repeats. A delivery that met a draining
         * registration waits [RpcDefaults.DRAIN_REDELIVERY_DELAY] instead. Throws [IllegalStateException] for
         * [CallFlow.CORE], which has no deliveries.
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
            val delay = when (cause) {
                DeliveryCause.DRAINING -> RpcDefaults.DRAIN_REDELIVERY_DELAY
                else -> backoff.after(delivered)
            }
            return when {
                cause == DeliveryCause.COMPLETED -> Ack
                cause.permanent || isLastDelivery(delivered) -> DeadLetter(retryDelay = delay)
                else -> Nak(delay)
            }
        }

        private fun ofPersistentRpc(
            cause: DeliveryCause,
            delivered: Long,
            timeLeft: Duration,
            backoff: List<Duration>,
        ): DeliveryDisposition = when (cause) {
            DeliveryCause.TIMED_OUT, DeliveryCause.NOT_ADMITTED, DeliveryCause.EXPIRED -> Term
            DeliveryCause.DRAINING -> redeliverOr(Term, RpcDefaults.DRAIN_REDELIVERY_DELAY, delivered, timeLeft)
            DeliveryCause.REDELIVERY_REQUESTED -> redeliverOr(Ack, backoff.after(delivered), delivered, timeLeft)
            else -> Ack
        }

        private fun redeliverOr(
            fallback: DeliveryDisposition,
            delay: Duration,
            delivered: Long,
            timeLeft: Duration,
        ): DeliveryDisposition = if (isLastDelivery(delivered) || delay >= timeLeft) fallback else Nak(delay)

        private fun isLastDelivery(delivered: Long): Boolean = delivered >= RpcDefaults.MAX_DELIVER

        private fun List<Duration>.after(delivered: Long): Duration =
            this[(delivered - 1).toInt().coerceIn(0, lastIndex)]
    }
}
