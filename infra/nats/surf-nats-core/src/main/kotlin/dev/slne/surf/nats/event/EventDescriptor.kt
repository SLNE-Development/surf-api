package dev.slne.surf.nats.event

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsEvent
import dev.slne.surf.nats.WireFormat
import dev.slne.surf.nats.codec.NatsCodec
import dev.slne.surf.nats.subject.Subjects
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

@InternalNatsApi
internal class EventDescriptor(
    val type: Class<*>,
    val subject: String,
    val format: WireFormat,
    val serializer: KSerializer<Any>,
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
            val subject = if (annotation.prefixed) "$clientName.$localSubject" else localSubject

            val format = annotation.format
            val serializer = codec.serializersModule(format).serializer(type)

            return EventDescriptor(
                type,
                Subjects.requireValidSubject(subject, type.name),
                format,
                serializer
            )
        }
    }
}