package dev.slne.surf.nats.runtime.rpc

import dev.slne.surf.nats.InternalNatsApi

/**
 * Kind of a reply to an `ALL_INSTANCES` call with a result ([classifyAllReply]).
 */
@InternalNatsApi
internal enum class AllReply {
    /**
     * Status [NO_RESPONDERS_STATUS]: no instance serves the method.
     */
    NO_RESPONDERS,

    /**
     * Any other NATS status message.
     */
    OTHER_STATUS,

    /**
     * The reply carries `Surf-Error`.
     */
    REMOTE_ERROR,

    /**
     * The instance answered `Surf-Answer: unknown` instead of a value.
     */
    UNKNOWN,

    /**
     * The reply body holds the answer value.
     */
    ANSWER;

    companion object {
        const val NO_RESPONDERS_STATUS = 503
    }
}
