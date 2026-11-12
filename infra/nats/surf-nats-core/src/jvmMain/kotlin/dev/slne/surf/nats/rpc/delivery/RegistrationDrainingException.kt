package dev.slne.surf.nats.rpc.delivery

import dev.slne.surf.nats.InternalNatsApi
import java.io.Serial

/**
 * Records in a dead letter that the last delivery of a fire call met a draining service registration before it got
 * a handler slot.
 *
 * Never thrown. The dead letter carries only the fully qualified name of this class in its `Surf-Error` header, so
 * that name is part of the dead-letter contract. The message text appears only in the responder log.
 *
 * The delivery waited for a slot when the registration started closing, or arrived afterwards while no slot was free.
 * Earlier deliveries are redelivered after [dev.slne.surf.nats.rpc.RpcDefaults.DRAIN_REDELIVERY_DELAY], so another
 * instance can take them over, and only the last one ([dev.slne.surf.nats.rpc.RpcDefaults.MAX_DELIVER]) is
 * dead-lettered. The handler does not run. A persistent RPC is redelivered or terminated without a dead letter
 * instead.
 */
@InternalNatsApi
internal class RegistrationDrainingException(message: String) : RuntimeException(message) {
    companion object {
        @Serial
        private const val serialVersionUID: Long = -69522305591015155L
    }
}
