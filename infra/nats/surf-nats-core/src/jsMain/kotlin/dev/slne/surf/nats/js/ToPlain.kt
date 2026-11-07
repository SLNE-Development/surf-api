@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.api.MessageSchema
import dev.slne.surf.nats.js.api.ProtoMessage
import dev.slne.surf.nats.js.external.protobuf.reflect.reflect
import dev.slne.surf.nats.js.interop.asDesc
import dev.slne.surf.nats.js.interop.messageOf
import dev.slne.surf.nats.js.plain.PlainConverter

/**
 * Converts a protobuf-es [message] of [schema] into plain data that JSON and superjson can carry.
 *
 * `google.protobuf.Timestamp` becomes a `Date`, `surf.types.v1.Uuid` a UUID string and a 64-bit integer a number, or
 * a decimal string with `jstype = JS_STRING`. A `google/protobuf/wrappers.proto` type becomes its value, and
 * `google.protobuf.Struct`, `Value` and `ListValue` become JSON values; a `Value` of kind `null_value` becomes `null`.
 * A field with explicit presence (every proto2 singular field and every message field) is `null` while unset
 * instead of its protobuf default; lists and maps are never `null`. A oneof becomes `{ case, value }` with the local
 * field name as `case`, or `{ case: null, value: null }` while unset. Messages lose `$typeName` and `$unknown`;
 * nested messages, lists and maps are converted the same way and map keys become strings. Enums stay numbers and
 * bytes stay `Uint8Array`.
 *
 * @throws IllegalArgumentException if [message] is not a message of [schema]
 * @throws RangeError if a 64-bit integer is outside the safe integer range of a number (±(2^53 − 1)), a timestamp
 * is outside the range of a `Date` or a UUID does not have 16 bytes
 */
@JsExport
fun toPlain(schema: MessageSchema, message: ProtoMessage): Any? {
    val desc = schema.asDesc()
    val typed = requireNotNull(messageOf(desc, message)) { "Expected a message of ${desc.typeName}" }
    return PlainConverter.message(reflect(desc, typed))
}
