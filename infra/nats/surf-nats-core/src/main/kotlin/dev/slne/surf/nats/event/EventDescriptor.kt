package dev.slne.surf.nats.event

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsFormat
import dev.slne.surf.nats.WireFormat
import dev.slne.surf.nats.codec.NatsCodec
import dev.slne.surf.nats.codec.WireSerializers
import dev.slne.surf.nats.subject.Subjects
import kotlinx.serialization.serializer

@InternalNatsApi
internal class EventDescriptor(
    val type: Class<*>,
    val subject: String,
    val format: WireFormat,
    val serializers: WireSerializers<Any>,
) {
    companion object {
        fun of(type: Class<*>, clientName: String, codec: NatsCodec): EventDescriptor {
            val annotation = requireNotNull(type.getAnnotation(NatsEvent::class.java)) {
                "${type.name} is not annotated with @NatsEvent"
            }

            val localSubject = annotation.subject.ifEmpty {
                val simpleName = type.simpleName
                require(simpleName.isNotEmpty()) { "${type.name} needs an explicit subject as it does not have a simple name" }
                Subjects.kebabCase(simpleName)
            }
            val subject = if (annotation.global) localSubject else "$clientName.$localSubject"

            val format = type.getAnnotation(NatsFormat::class.java)?.format ?: WireFormat.CBOR
            val serializers = WireSerializers.of { codec.serializersModule(it).serializer(type) }

            return EventDescriptor(
                type,
                Subjects.requireValidEventSubject(subject, type.name),
                format,
                serializers
            )
        }
    }
}