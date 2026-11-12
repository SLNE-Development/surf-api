package dev.slne.surf.nats.rpc.delivery

import dev.slne.surf.nats.InternalNatsApi
import java.io.Serial

/**
 * Records in a dead letter that the `Surf-Deadline` of a fire call had passed when the delivery arrived.
 *
 * Never thrown. The dead letter carries only the fully qualified name of this class in its `Surf-Error` header, so
 * that name is part of the dead-letter contract. The message text is not recorded anywhere. The responder logs a
 * warning of its own when it drops an expired delivery.
 *
 * A fire expires only when it carries `Surf-Deadline`, which surf-nats does not send with its own fire calls, so this
 * marks a fire from an external publisher. The cause is permanent: the delivery is dead-lettered on arrival,
 * whichever delivery it is, and the handler does not run. An expired persistent RPC is terminated without a dead
 * letter instead.
 */
@InternalNatsApi
internal class DeadlineExpiredException(message: String) : RuntimeException(message) {
    companion object {
        @Serial
        private const val serialVersionUID: Long = -4100512526997305410L
    }
}
