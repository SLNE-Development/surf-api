package dev.slne.surf.nats.runtime.rpc

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.protocol.ProtocolHeaders

/**
 * Classifies a reply by its status code (`0` for none), its `Surf-Error` header ([error]) and its `Surf-Answer` header
 * ([answer]); the headers are read only for replies without a status.
 */
@InternalNatsApi
internal inline fun classifyAllReply(
    status: Int,
    error: () -> String?,
    answer: () -> String?,
): AllReply = when {
    status == AllReply.NO_RESPONDERS_STATUS -> AllReply.NO_RESPONDERS
    status != 0 -> AllReply.OTHER_STATUS
    error() != null -> AllReply.REMOTE_ERROR
    answer() == ProtocolHeaders.ANSWER_UNKNOWN -> AllReply.UNKNOWN
    else -> AllReply.ANSWER
}
