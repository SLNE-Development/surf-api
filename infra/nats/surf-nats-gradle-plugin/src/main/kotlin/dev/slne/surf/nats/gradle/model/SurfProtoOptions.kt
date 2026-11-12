package dev.slne.surf.nats.gradle.model

import com.squareup.wire.schema.Location
import com.squareup.wire.schema.Options
import com.squareup.wire.schema.ProtoMember
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Rpc
import com.squareup.wire.schema.Service
import dev.slne.surf.nats.gradle.validate.Problems
import dev.slne.surf.nats.protocol.MethodOptions
import dev.slne.surf.nats.protocol.ProtocolOptions
import dev.slne.surf.nats.protocol.ProtocolResolution
import dev.slne.surf.nats.protocol.RecipientsOption
import dev.slne.surf.nats.protocol.ResolvedMethod
import dev.slne.surf.nats.protocol.ResolvedService
import dev.slne.surf.nats.protocol.ServiceOptions
import java.math.BigInteger

/**
 * Reads custom Protobuf options defined in `surf/nats/v1/options.proto`.
 *
 * Wire provides scalar option values as literal text and message options as maps.
 * Since Wire already validates values against their Protobuf types, these readers
 * primarily convert the parsed values into their corresponding protocol models.
 *
 * Call timeouts are additionally validated against the Kotlin [Int] range.
 * Unknown recipients indicate that [RecipientsOption] does not recognize a
 * constant declared in the Protobuf schema.
 */
internal object SurfProtoOptions {

    /**
     * The `(surf.nats.v1.event)` message option.
     */
    val EVENT: ProtoMember =
        ProtoMember.get(Options.MESSAGE_OPTIONS, ProtocolOptions.qualified(ProtocolOptions.EVENT))

    /**
     * The `(surf.nats.v1.service)` service option.
     */
    val SERVICE: ProtoMember =
        ProtoMember.get(Options.SERVICE_OPTIONS, ProtocolOptions.qualified(ProtocolOptions.SERVICE))

    /**
     * The `(surf.nats.v1.method)` method option.
     */
    val METHOD: ProtoMember =
        ProtoMember.get(Options.METHOD_OPTIONS, ProtocolOptions.qualified(ProtocolOptions.METHOD))

    private val SERVICE_OPTIONS =
        ProtoType.get(ProtocolOptions.qualified(ProtocolOptions.SERVICE_OPTIONS))

    private val METHOD_OPTIONS =
        ProtoType.get(ProtocolOptions.qualified(ProtocolOptions.METHOD_OPTIONS))

    private val INT_MAX: BigInteger = BigInteger.valueOf(Int.MAX_VALUE.toLong())

    /**
     * Checks whether a Protobuf option has an explicitly assigned value.
     *
     * Checks for presence rather than the boolean value of the option.
     * An explicitly assigned `false` therefore still counts as set.
     *
     * @param options the Protobuf options to inspect
     * @param member the option extension to look up
     * @return `true` if the option is present, otherwise `false`
     */
    fun isSet(options: Options, member: ProtoMember): Boolean =
        options.get(member) != null

    /**
     * Checks whether a Protobuf message is marked as a NATS event.
     *
     * Reads the `(surf.nats.v1.event)` option and defaults to `false`
     * when the option is absent.
     *
     * @param options the message's Protobuf options
     * @return `true` if the event option is enabled
     */
    fun isEvent(options: Options): Boolean = flag(options, EVENT)

    /**
     * Resolves the `(surf.nats.v1.service)` options of [service].
     *
     * Values that cannot be read are reported to [problems] and count as unset.
     */
    fun service(service: Service, problems: Problems): ResolvedService = ProtocolResolution.service(
        serviceOptions(service.options, service.location, problems),
        service.type.toString(),
    )

    /**
     * Resolves the `(surf.nats.v1.method)` options of [rpc] against its [service].
     *
     * Values that cannot be read are reported to [problems] and count as unset.
     */
    fun method(rpc: Rpc, service: ResolvedService, problems: Problems): ResolvedMethod = ProtocolResolution.method(
        methodOptions(rpc.options, rpc.location, problems),
        service,
        emptyResponse = rpc.responseType == ProtoType.EMPTY,
        streaming = rpc.requestStreaming || rpc.responseStreaming,
    )

    /**
     * Parses the NATS configuration declared on a Protobuf service.
     *
     * Reads `(surf.nats.v1.service)` and converts its call timeout,
     * recipients, and persistence settings into [ServiceOptions].
     *
     * An absent extension produces an empty [ServiceOptions] instance.
     * Invalid call timeouts or unknown recipients are reported through
     * [problems] and treated as unset.
     *
     * @return the parsed service configuration
     */
    private fun serviceOptions(options: Options, location: Location, problems: Problems): ServiceOptions {
        val values = options.get(SERVICE) as? Map<*, *>? ?: return ServiceOptions()
        val reader = Reader(SERVICE_OPTIONS, values, location, problems)

        return ServiceOptions(
            callTimeoutSeconds = reader.timeoutSeconds(ProtocolOptions.CALL_TIMEOUT_SECONDS),
            recipients = reader.recipients(ProtocolOptions.RECIPIENTS),
            persistent = reader.boolean(ProtocolOptions.PERSISTENT),
        )
    }

    /**
     * Parses the NATS configuration declared on an RPC method.
     *
     * Reads `(surf.nats.v1.method)` and converts its persistence,
     * call timeout, and recipients settings into [MethodOptions].
     *
     * An absent extension produces an empty [MethodOptions] instance.
     * Invalid call timeouts or unknown recipients are reported through
     * [problems] and treated as unset.
     *
     * @return the parsed method configuration
     */
    private fun methodOptions(options: Options, location: Location, problems: Problems): MethodOptions {
        val values = options.get(METHOD) as? Map<*, *>? ?: return MethodOptions()
        val reader = Reader(METHOD_OPTIONS, values, location, problems)

        return MethodOptions(
            persistent = reader.boolean(ProtocolOptions.PERSISTENT),
            callTimeoutSeconds = reader.timeoutSeconds(ProtocolOptions.CALL_TIMEOUT_SECONDS),
            recipients = reader.recipients(ProtocolOptions.RECIPIENTS),
        )
    }

    /**
     * Reads a boolean Protobuf option.
     *
     * Converts the parsed value using strict boolean parsing and returns
     * `false` when the option is absent.
     *
     * @return the configured boolean value, or `false` if absent
     */
    private fun flag(options: Options, member: ProtoMember): Boolean =
        options.get(member)?.toString()?.toBooleanStrict() ?: false

    /**
     * Converts fields of a parsed Protobuf option message into protocol values.
     *
     * Looks up fields using their fully qualified [ProtoMember] identifiers
     * and converts the values supplied by Wire.
     *
     * Missing fields return `null`. Invalid timeouts and unknown recipients
     * are reported through the provided problem collector.
     *
     * @property type the Protobuf message type containing the option fields
     * @property values the parsed option fields returned by Wire
     * @property location the source location used when reporting errors
     * @property problems the collector receiving conversion errors
     */
    private class Reader(
        private val type: ProtoType,
        private val values: Map<*, *>,
        private val location: Location,
        private val problems: Problems,
    ) {

        /**
         * Looks up a field in the parsed option message.
         *
         * @return the raw parsed value, or `null` if absent
         */
        private fun raw(field: String): Any? =
            values[ProtoMember.get(type, field)]

        /**
         * Reads an optional boolean field.
         *
         * Wire has already validated the Protobuf boolean value, so
         * strict parsing is sufficient.
         *
         * @param field the Protobuf field name
         * @return the boolean value, or `null` if absent
         */
        fun boolean(field: String): Boolean? =
            raw(field)?.toString()?.toBooleanStrict()

        /**
         * Reads a call timeout expressed in seconds.
         *
         * Accepts integer values between zero and [Int.MAX_VALUE],
         * inclusive. Values outside this range or invalid integer
         * literals are reported and treated as unset.
         *
         * @param field the Protobuf field containing the timeout
         * @return the timeout in seconds, or `null` if absent or invalid
         */
        fun timeoutSeconds(field: String): Int? {
            val value = raw(field) ?: return null
            val seconds = DefaultValues.integer(value.toString(), BigInteger.ZERO, INT_MAX)

            if (seconds == null) {
                problems.error(
                    location,
                    "option ${type.simpleName}.$field must be between 0 and ${Int.MAX_VALUE}, was '$value'",
                )
                return null
            }

            return seconds.toInt()
        }

        /**
         * Resolves a recipients enum value from a Protobuf option field.
         *
         * Wire validates the value against the Protobuf enum definition.
         * An unknown value therefore indicates that [RecipientsOption]
         * is out of sync with the constants declared in `options.proto`.
         *
         * Unknown values are reported and treated as unset.
         *
         * @param field the Protobuf field containing the recipients value
         * @return the corresponding recipients option, or `null` if absent or unknown
         */
        fun recipients(field: String): RecipientsOption? {
            val value = raw(field)?.toString() ?: return null

            // Wire accepts exactly the constants of NatsRecipients, so this fails only if RecipientsOption lags behind.
            val option = RecipientsOption.ofName(value)

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
