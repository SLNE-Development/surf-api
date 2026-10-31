package dev.slne.surf.nats.event

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.descriptor.GeneratedDescriptors
import dev.slne.surf.nats.protocol.ProtocolNaming
import dev.slne.surf.nats.protocol.ProtocolSubjects
import kotlinx.serialization.KSerializer

@InternalNatsApi
internal class EventDescriptor(
    val type: Class<*>,
    val subject: String,
    val serializer: KSerializer<Any>,
) {
    companion object {
        /**
         * Reads the generated descriptor of [type] and derives its subject from the proto full name.
         *
         * Throws [IllegalArgumentException] when [type] is no generated event or its proto name is invalid.
         */
        @Suppress("UNCHECKED_CAST")
        fun of(type: Class<*>): EventDescriptor {
            val generated = GeneratedDescriptors.event(type)
            ProtocolNaming.nameProblem("event", generated.protoName)?.let { problem ->
                throw IllegalArgumentException("Invalid NATS_DESCRIPTOR of ${type.name}: $problem")
            }

            return EventDescriptor(
                type,
                ProtocolSubjects.event(generated.protoName),
                generated.serializer as KSerializer<Any>,
            )
        }
    }
}
