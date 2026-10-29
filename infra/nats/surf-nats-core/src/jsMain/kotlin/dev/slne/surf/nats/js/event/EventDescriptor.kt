package dev.slne.surf.nats.js.event

import dev.slne.surf.nats.js.descriptor.PerOwner
import dev.slne.surf.nats.js.descriptor.ProtocolOptionReader
import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.js.interop.getOrPut
import dev.slne.surf.nats.protocol.EventOptions
import dev.slne.surf.nats.protocol.ProtocolNaming
import dev.slne.surf.nats.protocol.ProtocolOptions
import dev.slne.surf.nats.protocol.ProtocolResolution
import dev.slne.surf.nats.protocol.ProtocolSubjects
import js.collections.WeakMap

/**
 * A surf-nats event resolved from its protobuf-es schema; the owner of a non-global event is named per call.
 */
internal class EventDescriptor(
    val schema: DescMessage,
    val subject: String, // event subject of the option or the kebab-case message name, without owner and version
    val global: Boolean,
    val majorVersion: Int,
) {
    val typeName: String get() = schema.typeName

    private val subjects = PerOwner(global, typeName) { owner ->
        ProtocolSubjects.event(owner, subject, majorVersion)
    }

    fun subject(owner: String?): String = subjects[owner] // <owner>.v<N>.<subject>

    companion object {
        private val descriptors = WeakMap<DescMessage, EventDescriptor>()

        fun of(schema: DescMessage): EventDescriptor = descriptors.getOrPut(schema) { resolve(schema) }

        private fun resolve(schema: DescMessage): EventDescriptor {
            val options = ProtocolOptionReader.event(schema)
            require(options.isSet) {
                "${schema.typeName} is not a surf-nats event: it has no (${ProtocolOptions.qualified(ProtocolOptions.EVENT)}) option"
            }

            val resolved = ProtocolResolution.event(
                EventOptions(
                    subject = options.string(ProtocolOptions.SUBJECT),
                    global = options.boolean(ProtocolOptions.GLOBAL),
                ),
                schema.name,
            )
            val problems = resolved.problems(schema.typeName)
            require(problems.isEmpty()) { problems.joinToString("; ") }

            return EventDescriptor(
                schema,
                resolved.subject,
                resolved.global,
                ProtocolNaming.majorVersion(schema.file.proto.`package`, schema.typeName),
            )
        }
    }
}
