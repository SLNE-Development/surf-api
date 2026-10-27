package dev.slne.surf.nats.js.contract

import dev.slne.surf.nats.js.external.protobuf.reflect.ReflectMessage
import dev.slne.surf.nats.js.interop.FieldShape
import dev.slne.surf.nats.protocol.ProtocolOptions
import dev.slne.surf.nats.protocol.RecipientsOption

/**
 * The scalar and enum fields of one surf option message; every getter returns `null` for an unset field.
 */
internal class OptionValues(private val message: ReflectMessage?) {
    val isSet: Boolean get() = message != null

    fun string(name: String): String? = value(name) as String?

    fun boolean(name: String): Boolean? = value(name) as Boolean?

    fun int(name: String): Int? = value(name) as Int?

    fun recipients(): RecipientsOption? = int(ProtocolOptions.RECIPIENTS)?.let(RecipientsOption::ofNumber)

    private fun value(name: String): Any? {
        val message = message ?: return null
        val field = message.fields.find { it.name == name } ?: return null
        if (!message.isSet(field)) return null

        return when (val shape = FieldShape.of(field)) {
            is FieldShape.Scalar -> message.get(shape.field)
            is FieldShape.Enum -> message.get(shape.field)
            else -> null
        }
    }

    companion object {
        val UNSET = OptionValues(null)
    }
}
