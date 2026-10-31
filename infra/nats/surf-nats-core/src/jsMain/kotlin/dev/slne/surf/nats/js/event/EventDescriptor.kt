package dev.slne.surf.nats.js.event

import dev.slne.surf.nats.js.descriptor.ProtocolOptionReader
import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.js.interop.getOrPut
import dev.slne.surf.nats.protocol.ProtocolNaming
import dev.slne.surf.nats.protocol.ProtocolOptions
import dev.slne.surf.nats.protocol.ProtocolSubjects
import js.collections.WeakMap

/**
 * A surf-nats event resolved from its protobuf-es schema; [subject] is `surf.evt.<message full name>`.
 */
internal class EventDescriptor private constructor(
    val schema: DescMessage,
    val subject: String,
) {
    val typeName: String get() = schema.typeName

    companion object {
        private val descriptors = WeakMap<DescMessage, EventDescriptor>()

        fun of(schema: DescMessage): EventDescriptor = descriptors.getOrPut(schema) { resolve(schema) }

        private fun resolve(schema: DescMessage): EventDescriptor {
            require(ProtocolOptionReader.isEvent(schema)) {
                "${schema.typeName} is not a surf-nats event: it has no (${ProtocolOptions.qualified(ProtocolOptions.EVENT)}) = true option"
            }

            ProtocolNaming.nameProblem("event", schema.typeName, schema.name)?.let { throw IllegalArgumentException(it) }
            ProtocolNaming.majorVersion(schema.file.proto.`package`, schema.typeName)

            return EventDescriptor(schema, ProtocolSubjects.event(schema.typeName))
        }
    }
}
