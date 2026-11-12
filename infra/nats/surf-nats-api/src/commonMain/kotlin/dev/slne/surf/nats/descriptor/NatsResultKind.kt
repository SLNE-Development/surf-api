package dev.slne.surf.nats.descriptor

/**
 * Describes what a generated service method returns.
 *
 * The Gradle plugin derives the result kind from the rpc: [UNIT] for a `google.protobuf.Empty` response, [ANSWER]
 * for an `ALL_INSTANCES` rpc with another response, [VALUE] otherwise. Together with
 * [NatsMethodDescriptor.persistent] and [NatsMethodDescriptor.recipients], the result kind selects the delivery
 * semantics of a call, so changing the response type or the recipients of an rpc changes how it is delivered.
 */
public enum class NatsResultKind {
    /**
     * Returns [Unit]. A persistent method with this result kind is a fire call: the caller waits until the server has
     * stored the message, not for the handler.
     */
    UNIT,

    /**
     * Returns the response message.
     */
    VALUE,

    /**
     * Returns the response message wrapped in [dev.slne.surf.nats.rpc.Answer], so that a responder can answer that it
     * does not know the result.
     */
    ANSWER,
}
