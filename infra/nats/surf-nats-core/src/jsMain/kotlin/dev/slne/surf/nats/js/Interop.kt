package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.external.protobuf.AnyDesc
import dev.slne.surf.nats.js.external.protobuf.DescExtension
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
import dev.slne.surf.nats.js.external.protobuf.DescFieldSingular
import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.js.external.protobuf.DescMethod
import dev.slne.surf.nats.js.external.protobuf.getOption
import dev.slne.surf.nats.js.external.protobuf.isMessage
import dev.slne.surf.nats.js.external.protobuf.reflect.ReflectMessage
import dev.slne.surf.nats.js.external.protobuf.reflect.reflect
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

// Boundary between untyped JS values and the typed externals. The casts of the JS runtime live here.

// Kotlin trusts the non-null types of exported declarations, but JS callers may pass null or undefined there.
internal fun <T : Any> requireFromJs(value: T?, name: String): T = requireNotNull(value) { "$name is required" }

// An optional timeout option in milliseconds; [name] labels it in the error for values below 1.
internal fun positiveTimeoutOrNull(timeoutMs: Int?, name: String): Duration? = timeoutMs?.let {
    require(it > 0) { "$name must be positive, was $it ms" }
    it.milliseconds
}

internal fun MessageSchema.asDesc(): DescMessage {
    require(requireFromJs(this, "schema").kind == "message") { "Expected a protobuf-es message schema (GenMessage)" }
    return unsafeCast<DescMessage>()
}

internal fun MethodSchema.asDesc(): DescMethod {
    require(requireFromJs(this, "method").kind == "rpc") { "Expected a protobuf-es service method (Service.method.x)" }
    return unsafeCast<DescMethod>()
}

// The message itself when [value] is a message of [schema] (isMessage is a TypeScript type guard), otherwise null.
internal fun messageOf(schema: DescMessage, value: Any?): ProtoMessage? {
    return if (isMessage(value, schema)) value.unsafeCast<ProtoMessage>() else null
}

private external interface ThenableShape {
    val then: Any?
}

// Whether await adopts [value]: an object or function with a callable `then`. Reading `then` may throw.
internal fun isThenable(value: Any?): Boolean {
    val type = jsTypeOf(value)
    return value != null && (type == "object" || type == "function") &&
            jsTypeOf(value.unsafeCast<ThenableShape>().then) == "function"
}

// The value of a message-typed option; [element] must have the option set (hasOption).
internal fun optionMessage(element: AnyDesc, extension: DescExtension): ReflectMessage {
    val type = checkNotNull(extension.message) { "${extension.typeName} is not a message option" }
    val value = checkNotNull(messageOf(type, getOption(element, extension))) {
        "Option ${extension.typeName} is not a ${type.typeName} message"
    }
    return reflect(type, value)
}

// The variant of a DescField, discriminated by fieldKind and listKind/mapKind like the protobuf-es union.
internal sealed interface FieldShape {
    sealed interface Singular : FieldShape {
        val field: DescFieldSingular
    }

    class Scalar(override val field: DescFieldScalar) : Singular
    class Enum(override val field: DescFieldEnum) : Singular
    class Message(override val field: DescFieldMessage) : Singular
    class EnumList(val field: DescFieldEnumList) : FieldShape
    class MessageList(val field: DescFieldMessageList) : FieldShape
    class EnumMap(val field: DescFieldEnumMap) : FieldShape
    class MessageMap(val field: DescFieldMessageMap) : FieldShape

    // A list or map of scalars
    data object Scalars : FieldShape
}

internal fun DescField.shape(): FieldShape = when (fieldKind) {
    "scalar" -> FieldShape.Scalar(unsafeCast<DescFieldScalar>())
    "enum" -> FieldShape.Enum(unsafeCast<DescFieldEnum>())
    "message" -> FieldShape.Message(unsafeCast<DescFieldMessage>())
    "list" -> when (unsafeCast<DescFieldList>().listKind) {
        "enum" -> FieldShape.EnumList(unsafeCast<DescFieldEnumList>())
        "message" -> FieldShape.MessageList(unsafeCast<DescFieldMessageList>())
        else -> FieldShape.Scalars
    }

    "map" -> when (unsafeCast<DescFieldMap>().mapKind) {
        "enum" -> FieldShape.EnumMap(unsafeCast<DescFieldEnumMap>())
        "message" -> FieldShape.MessageMap(unsafeCast<DescFieldMessageMap>())
        else -> FieldShape.Scalars
    }

    else -> throw IllegalStateException("Unknown fieldKind '$fieldKind' of ${parent.typeName}.$name")
}
