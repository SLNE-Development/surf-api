package dev.slne.surf.nats.js.codec

import dev.slne.surf.nats.js.api.ProtoMessage
import dev.slne.surf.nats.js.api.ProtoMessageInit
import dev.slne.surf.nats.js.codec.rules.MessageRules
import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.js.external.protobuf.fromBinary
import dev.slne.surf.nats.js.external.protobuf.toBinary
import dev.slne.surf.nats.protocol.ProtocolHeaders
import js.typedarrays.Uint8Array

/**
 * Encodes and decodes protobuf-es messages under the presence and enum rules of the Kotlin codec ([MessageRules]).
 */
internal object NatsCodec {
    /**
     * Encodes [value], a message, an init object or `null`, after the outgoing checks of [MessageRules.prepare].
     *
     * @throws IllegalArgumentException if [value] violates the rules or is a message of another type
     */
    fun encode(schema: DescMessage, value: ProtoMessageInit?): Uint8Array<*> {
        return toBinary(schema, MessageRules.prepare(schema, value))
    }

    /**
     * Decodes [data] as [schema] and normalizes unknown enum numbers. [source] names the message in the error of an
     * undecodable one.
     *
     * @throws IllegalArgumentException if [contentType] is not protobuf or the message violates the rules
     */
    fun decode(schema: DescMessage, data: Uint8Array<*>, contentType: String?, source: String): ProtoMessage {
        ProtocolHeaders.requireProtobufContentType(contentType)
        val message = fromBinary(schema, data)
        val violation = MessageRules.normalizeAndCheck(schema, message)

        require(violation == null) {
            "Undecodable ${schema.typeName} ($source): $violation"
        }

        return message
    }
}
