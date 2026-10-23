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
        @Suppress("UNCHECKED_CAST")
        fun of(type: Class<*>, clientName: String): EventDescriptor {
            val generated = GeneratedDescriptors.event(type)
            val subject = ProtocolSubjects.event(
                if (generated.global) null else clientName,
                generated.subject,
                generated.majorVersion,
            )

            return EventDescriptor(
                type,
                ProtocolNaming.requireValidEventSubject(subject, type.name),
                generated.serializer as KSerializer<Any>,
            )
        }
    }
}
