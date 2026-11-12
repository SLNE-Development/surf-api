@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.slne.surf.nats.js.plain

import dev.slne.surf.nats.js.external.protobuf.DescField
import dev.slne.surf.nats.js.external.protobuf.DescFieldSingular
import dev.slne.surf.nats.js.external.protobuf.DescOneof
import dev.slne.surf.nats.js.external.protobuf.reflect.ReflectMessage
import dev.slne.surf.nats.js.interop.FieldShape
import dev.slne.surf.nats.js.uuidToString
import dev.slne.surf.nats.protocol.ProtocolFieldRules
import js.array.ReadonlyArray
import js.array.toJsArray
import js.date.Date
import js.errors.RangeError
import js.iterable.JsIterator
import js.iterable.iterator
import js.numbers.BigInt
import js.objects.Record
import js.objects.recordOf

/**
 * Converts protobuf-es messages into the plain values of `toPlain`, reading them through protobuf-es reflection.
 */
internal object PlainConverter {
    private const val TIMESTAMP_TYPE = "google.protobuf.Timestamp"
    private const val STRUCT_TYPE = "google.protobuf.Struct"
    private const val VALUE_TYPE = "google.protobuf.Value"
    private const val LIST_VALUE_TYPE = "google.protobuf.ListValue"
    private const val NULL_VALUE_FIELD = "nullValue"
    private const val PRESENCE_IMPLICIT = 2
    private const val MILLIS_PER_SECOND = 1000L
    private const val NANOS_PER_MILLI = 1_000_000

    // ECMAScript Date range: ±8.64e15 ms
    private const val MAX_DATE_SECONDS = 8_640_000_000_000L

    // Number.MAX_SAFE_INTEGER
    private const val MAX_SAFE_INTEGER = (1L shl 53) - 1

    // ScalarType values of the 64-bit integers: INT64, UINT64, FIXED64, SFIXED64, SINT64
    private val LONG_SCALARS = intArrayOf(3, 4, 6, 16, 18)

    // google/protobuf/wrappers.proto: each type holds its value in its only field
    private val WRAPPER_TYPES = setOf(
        "google.protobuf.DoubleValue",
        "google.protobuf.FloatValue",
        "google.protobuf.Int64Value",
        "google.protobuf.UInt64Value",
        "google.protobuf.Int32Value",
        "google.protobuf.UInt32Value",
        "google.protobuf.BoolValue",
        "google.protobuf.StringValue",
        "google.protobuf.BytesValue",
    )

    /**
     * Converts [message]. Only a `google.protobuf.Value` can become `null`.
     */
    fun message(message: ReflectMessage): JsAny? = when (message.desc.typeName) {
        TIMESTAMP_TYPE -> timestamp(message)
        ProtocolFieldRules.UUID_TYPE -> uuidToString(message.message).toJsString()
        VALUE_TYPE -> jsonValue(message)
        // Struct holds its map of Values, ListValue its list of Values, a wrapper its value
        STRUCT_TYPE, LIST_VALUE_TYPE, in WRAPPER_TYPES -> field(
            message,
            message.desc.fields.single()
        )

        else -> fields(message)
    }

    private fun fields(message: ReflectMessage): Record<JsString, JsAny?> {
        val plain = recordOf<JsString, JsAny?>()
        for (field in message.desc.fields) {
            if (field.oneof == null) plain[field.localName] = field(message, field)
        }
        for (oneof in message.desc.oneofs) {
            plain[oneof.localName] = oneof(message, oneof)
        }
        return plain
    }

    private fun field(message: ReflectMessage, field: DescField): JsAny? {
        return when (val shape = FieldShape.of(field)) {
            is FieldShape.Scalar -> singular(message, shape.field) {
                scalar(message.get(shape.field), shape.field.scalar, shape.field.longAsString)
            }

            is FieldShape.Enum -> singular(message, shape.field) {
                message.get(shape.field).toJsNumber()
            }

            is FieldShape.Message -> singular(
                message,
                shape.field
            ) { message(message.get(shape.field)) }

            is FieldShape.ScalarList -> {
                val list = message.get(shape.field)
                array(list.size) {
                    scalar(
                        list.get(it)!!,
                        shape.field.scalar,
                        shape.field.longAsString
                    )
                }
            }

            is FieldShape.EnumList -> {
                val list = message.get(shape.field)
                array(list.size) { list.get(it)!!.toJsNumber() }
            }

            is FieldShape.MessageList -> {
                val list = message.get(shape.field)
                array(list.size) { message(list.get(it)!!) }
            }

            is FieldShape.ScalarMap -> {
                val map = message.get(shape.field)
                // protobuf-es ignores jstype on map values
                record(map.keys()) {
                    scalar(
                        map.get(it)!!,
                        shape.field.scalar,
                        longAsString = false
                    )
                }
            }

            is FieldShape.EnumMap -> {
                val map = message.get(shape.field)
                record(map.keys()) { map.get(it)!!.toJsNumber() }
            }

            is FieldShape.MessageMap -> {
                val map = message.get(shape.field)
                record(map.keys()) { message(map.get(it)!!) }
            }
        }
    }

    // Fields with explicit presence (proto2 optional and required, message fields) are null while unset
    private inline fun singular(
        message: ReflectMessage,
        field: DescFieldSingular,
        value: () -> JsAny?
    ): JsAny? {
        return if (field.presence != PRESENCE_IMPLICIT && !message.isSet(field)) null else value()
    }

    private fun oneof(message: ReflectMessage, oneof: DescOneof): Record<JsString, JsAny?> {
        val plain = recordOf<JsString, JsAny?>()
        val selected = message.oneofCase(oneof)
        plain["case"] = selected?.localName?.toJsString()
        plain["value"] = selected?.let { field(message, it) }
        return plain
    }

    private inline fun array(size: Int, element: (Int) -> JsAny?): ReadonlyArray<JsAny?> {
        return List(size, element).toJsArray()
    }

    private inline fun record(
        keys: JsIterator<Any>,
        value: (Any) -> JsAny?
    ): Record<JsString, JsAny?> {
        val plain = recordOf<JsString, JsAny?>()
        // Map keys are strings, numbers, booleans or bigints, and their string form is the key of the plain object
        for (key in keys) plain["$key"] = value(key)
        return plain
    }

    /**
     * Returns [value] unchanged, except that a 64-bit integer, a bigint or a decimal string, becomes a number, or a
     * decimal string if [longAsString] (`jstype = JS_STRING`) is set.
     */
    private fun scalar(value: Any, scalarType: Int, longAsString: Boolean): JsAny {
        if (scalarType !in LONG_SCALARS) return value.unsafeCast<JsAny>()
        if (longAsString) return decimal(value).toJsString()
        return safeInteger(value).toDouble().toJsNumber()
    }

    // An unset kind is JSON null for protobuf-es, like the null_value kind
    private fun jsonValue(value: ReflectMessage): JsAny? {
        val kind = value.oneofCase(value.desc.oneofs.single())
        return if (kind == null || kind.localName == NULL_VALUE_FIELD) null else field(value, kind)
    }

    private fun decimal(value: Any): String {
        return if (jsTypeOf(value) == "bigint") value.unsafeCast<BigInt>()
            .toString(10) else value.toString()
    }

    private fun safeInteger(value: Any): Long {
        val decimal = decimal(value)
        val long = decimal.toLongOrNull()
        if (long == null || long > MAX_SAFE_INTEGER || long < -MAX_SAFE_INTEGER) {
            throw RangeError("The 64-bit integer $decimal does not fit into a number without losing precision")
        }
        return long
    }

    private fun timestamp(timestamp: ReflectMessage): Date {
        val fields = timestamp.desc.fields
        val seconds =
            safeInteger(timestamp.get(fields.first { it.localName == "seconds" }.asScalar()))
        val nanos =
            timestamp.get(fields.first { it.localName == "nanos" }.asScalar()).unsafeCast<Int>()
        if (seconds > MAX_DATE_SECONDS || seconds < -MAX_DATE_SECONDS) {
            throw RangeError("The timestamp of $seconds seconds is outside the range of a Date")
        }
        return Date((seconds * MILLIS_PER_SECOND + nanos / NANOS_PER_MILLI).toDouble())
    }

    private fun DescField.asScalar() = (FieldShape.of(this) as FieldShape.Scalar).field
}
