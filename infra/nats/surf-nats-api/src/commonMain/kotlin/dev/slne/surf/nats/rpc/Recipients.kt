package dev.slne.surf.nats.rpc

/**
 * Selects one or all listening instances in a recipient group.
 *
 * Used by [dev.slne.surf.nats.event.NatsListener.recipients] and
 * [dev.slne.surf.nats.descriptor.NatsMethodDescriptor.recipients].
 */
public enum class Recipients {
    /**
     * Addresses one listening instance of the group.
     */
    ANY_INSTANCE,

    /**
     * Addresses every listening instance of the group.
     */
    ALL_INSTANCES
}
