package dev.slne.surf.nats.runtime.rpc

import dev.slne.surf.nats.InternalNatsApi

/**
 * Kind of a reply to an `ALL_INSTANCES` call with a result ([classifyAllReply]).
 */
@InternalNatsApi
internal enum class AllReply {
    NO_RESPONDERS,
    OTHER_STATUS,
    REMOTE_ERROR,
    UNKNOWN,
    ANSWER;

    companion object {
        const val NO_RESPONDERS_STATUS = 503
    }
}
