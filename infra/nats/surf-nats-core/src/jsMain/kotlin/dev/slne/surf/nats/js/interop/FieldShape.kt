package dev.slne.surf.nats.js.interop

import dev.slne.surf.nats.js.external.protobuf.DescField
import dev.slne.surf.nats.js.external.protobuf.DescFieldEnum
import dev.slne.surf.nats.js.external.protobuf.DescFieldEnumList
import dev.slne.surf.nats.js.external.protobuf.DescFieldEnumMap
import dev.slne.surf.nats.js.external.protobuf.DescFieldList
import dev.slne.surf.nats.js.external.protobuf.DescFieldMap
import dev.slne.surf.nats.js.external.protobuf.DescFieldMessage
import dev.slne.surf.nats.js.external.protobuf.DescFieldMessageList
import dev.slne.surf.nats.js.external.protobuf.DescFieldMessageMap
import dev.slne.surf.nats.js.external.protobuf.DescFieldScalar
import dev.slne.surf.nats.js.external.protobuf.DescFieldScalarList
import dev.slne.surf.nats.js.external.protobuf.DescFieldScalarMap

/**
 * Typed variant of a protobuf-es [DescField], discriminated like its `fieldKind`/`listKind`/`mapKind` union.
 */
internal sealed interface FieldShape {
    class Scalar(val field: DescFieldScalar) : FieldShape
    class Enum(val field: DescFieldEnum) : FieldShape
    class Message(val field: DescFieldMessage) : FieldShape
    class EnumList(val field: DescFieldEnumList) : FieldShape
    class MessageList(val field: DescFieldMessageList) : FieldShape
    class EnumMap(val field: DescFieldEnumMap) : FieldShape
    class MessageMap(val field: DescFieldMessageMap) : FieldShape
    class ScalarList(val field: DescFieldScalarList) : FieldShape
    class ScalarMap(val field: DescFieldScalarMap) : FieldShape

    companion object {
        fun of(field: DescField): FieldShape = when (field.fieldKind) {
            "scalar" -> Scalar(field.unsafeCast<DescFieldScalar>())
            "enum" -> Enum(field.unsafeCast<DescFieldEnum>())
            "message" -> Message(field.unsafeCast<DescFieldMessage>())
            "list" -> when (field.unsafeCast<DescFieldList>().listKind) {
                "enum" -> EnumList(field.unsafeCast<DescFieldEnumList>())
                "message" -> MessageList(field.unsafeCast<DescFieldMessageList>())
                else -> ScalarList(field.unsafeCast<DescFieldScalarList>())
            }

            "map" -> when (field.unsafeCast<DescFieldMap>().mapKind) {
                "enum" -> EnumMap(field.unsafeCast<DescFieldEnumMap>())
                "message" -> MessageMap(field.unsafeCast<DescFieldMessageMap>())
                else -> ScalarMap(field.unsafeCast<DescFieldScalarMap>())
            }

            else -> throw IllegalStateException("Unknown fieldKind '${field.fieldKind}' of ${field.parent.typeName}.${field.name}")
        }
    }
}
