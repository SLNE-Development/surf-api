package dev.slne.surf.nats.rpc.delivery

import dev.slne.surf.nats.InternalNatsApi
import java.io.Serial

/**
 * Records in a dead letter that the last delivery of a fire call got no handler slot before its deadline.
 *
 * Never thrown. The dead letter carries only the fully qualified name of this class in its `Surf-Error` header, so
 * that name is part of the dead-letter contract. The message text appears only in the responder log.
 *
 * The deadline is the `Surf-Deadline` of the request, else the arrival of the delivery plus the maximum fire
 * duration (10 minutes). A fire always waits for a slot of the long lane, so the cause can be an exhausted long share
 * while short slots are free. Earlier deliveries are redelivered with backoff, and only the last one
 * ([dev.slne.surf.nats.rpc.RpcDefaults.MAX_DELIVER]) is dead-lettered. The handler does not run. A persistent RPC
 * that gets no slot is terminated without a dead letter instead.
 */
@InternalNatsApi
internal class NoHandlerSlotException(message: String) : RuntimeException(message) {
    companion object {
        @Serial
        private const val serialVersionUID: Long = -4654516552028505268L
    }
}
