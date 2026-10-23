package dev.slne.surf.nats.gradle.model

import com.squareup.wire.schema.Location
import com.squareup.wire.schema.Options
import com.squareup.wire.schema.ProtoMember
import com.squareup.wire.schema.ProtoType
import dev.slne.surf.nats.gradle.validate.Problems
import dev.slne.surf.nats.protocol.EventOptions
import dev.slne.surf.nats.protocol.MethodOptions
import dev.slne.surf.nats.protocol.ProtocolOptions
import dev.slne.surf.nats.protocol.RecipientsOption
import dev.slne.surf.nats.protocol.ServiceOptions
import java.math.BigInteger

/**
 * Reads the options of `surf/nats/v1/options.proto`; wire delivers scalars as literal text and messages as maps.
 */
internal object SurfOptions {
    val EVENT: ProtoMember = ProtoMember.get(Options.MESSAGE_OPTIONS, ProtocolOptions.qualified(ProtocolOptions.EVENT))
    val SERVICE: ProtoMember = ProtoMember.get(Options.SERVICE_OPTIONS, ProtocolOptions.qualified(ProtocolOptions.SERVICE))
    val METHOD: ProtoMember = ProtoMember.get(Options.METHOD_OPTIONS, ProtocolOptions.qualified(ProtocolOptions.METHOD))
    val NULLABLE: ProtoMember = ProtoMember.get(Options.FIELD_OPTIONS, ProtocolOptions.qualified(ProtocolOptions.NULLABLE))

    private val EVENT_OPTIONS = ProtoType.get(ProtocolOptions.qualified(ProtocolOptions.EVENT_OPTIONS))
    private val SERVICE_OPTIONS = ProtoType.get(ProtocolOptions.qualified(ProtocolOptions.SERVICE_OPTIONS))
    private val METHOD_OPTIONS = ProtoType.get(ProtocolOptions.qualified(ProtocolOptions.METHOD_OPTIONS))
    private val INT_MAX: BigInteger = BigInteger.valueOf(Int.MAX_VALUE.toLong())

    fun isSet(options: Options, member: ProtoMember): Boolean = options.get(member) != null

    fun event(options: Options, location: Location, problems: Problems): EventOptions? {
        val values = messageValue(options, EVENT) ?: return null
        val reader = Reader(EVENT_OPTIONS, values, location, problems)
        return EventOptions(reader.string(ProtocolOptions.SUBJECT), reader.boolean(ProtocolOptions.GLOBAL))
    }

    fun service(options: Options, location: Location, problems: Problems): ServiceOptions {
        val values = messageValue(options, SERVICE) ?: return ServiceOptions()
        val reader = Reader(SERVICE_OPTIONS, values, location, problems)
        return ServiceOptions(
            name = reader.string(ProtocolOptions.NAME),
            global = reader.boolean(ProtocolOptions.GLOBAL),
            callTimeoutSeconds = reader.timeoutSeconds(ProtocolOptions.CALL_TIMEOUT_SECONDS),
            recipients = reader.recipients(ProtocolOptions.RECIPIENTS),
            persistent = reader.boolean(ProtocolOptions.PERSISTENT),
        )
    }

    fun method(options: Options, location: Location, problems: Problems): MethodOptions {
        val values = messageValue(options, METHOD) ?: return MethodOptions()
        val reader = Reader(METHOD_OPTIONS, values, location, problems)
        return MethodOptions(
            name = reader.string(ProtocolOptions.NAME),
            persistent = reader.boolean(ProtocolOptions.PERSISTENT),
            callTimeoutSeconds = reader.timeoutSeconds(ProtocolOptions.CALL_TIMEOUT_SECONDS),
            recipients = reader.recipients(ProtocolOptions.RECIPIENTS),
        )
    }

    fun nullable(options: Options, location: Location, problems: Problems): Boolean {
        val value = options.get(NULLABLE) ?: return false
        return parseBoolean(value, "(${ProtocolOptions.qualified(ProtocolOptions.NULLABLE)})", location, problems) ?: false
    }

    private fun messageValue(options: Options, member: ProtoMember): Map<*, *>? {
        return when (val value = options.get(member)) {
            null -> null
            is Map<*, *> -> value
            else -> emptyMap<Any, Any>()
        }
    }

    private fun parseBoolean(value: Any, name: String, location: Location, problems: Problems): Boolean? {
        val result = value.toString().toBooleanStrictOrNull()
        if (result == null) problems.error(location, "option $name must be true or false, was '$value'")
        return result
    }

    private class Reader(
        private val type: ProtoType,
        private val values: Map<*, *>,
        private val location: Location,
        private val problems: Problems,
    ) {
        private fun raw(field: String): Any? = values[ProtoMember.get(type, field)]

        fun string(field: String): String? = raw(field)?.toString()

        fun boolean(field: String): Boolean? {
            val value = raw(field) ?: return null
            return parseBoolean(value, "${type.simpleName}.$field", location, problems)
        }

        fun timeoutSeconds(field: String): Int? {
            val value = raw(field) ?: return null
            val seconds = DefaultValues.integer(value.toString(), BigInteger.ZERO, INT_MAX)
            if (seconds == null) {
                problems.error(location, "option ${type.simpleName}.$field must be between 0 and ${Int.MAX_VALUE}, was '$value'")
                return null
            }
            return seconds.toInt()
        }

        fun recipients(field: String): RecipientsOption? {
            val value = raw(field)?.toString() ?: return null
            val option = RecipientsOption.ofName(value) ?: value.toIntOrNull()?.let(RecipientsOption::ofNumber)
            if (option == null) {
                problems.error(
                    location,
                    "option ${type.simpleName}.$field has unknown value '$value', expected one of " +
                            RecipientsOption.entries.joinToString { it.protoName },
                )
            }
            return option
        }
    }
}
