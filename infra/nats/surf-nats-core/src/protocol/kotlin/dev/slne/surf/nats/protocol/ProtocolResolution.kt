package dev.slne.surf.nats.protocol

/**
 * Values of the `surf.nats.v1.NatsRecipients` proto enum.
 *
 * Entry names are the proto constant names without the `NATS_RECIPIENTS_` prefix. [protoName] is the full constant
 * name and [number] the proto enum number.
 */
internal enum class RecipientsOption(val number: Int, val protoName: String) {
    UNSPECIFIED(0, "NATS_RECIPIENTS_UNSPECIFIED"),
    ANY_INSTANCE(1, "NATS_RECIPIENTS_ANY_INSTANCE"),
    ALL_INSTANCES(2, "NATS_RECIPIENTS_ALL_INSTANCES");

    companion object {
        private val byNumber = entries.associateBy { it.number }
        private val byName = entries.associateBy { it.protoName }

        /**
         * Resolves a proto enum number, or returns `null` for an unknown number.
         */
        fun ofNumber(number: Int): RecipientsOption? = byNumber[number]

        /**
         * Resolves a full proto constant name such as `NATS_RECIPIENTS_ANY_INSTANCE`, or returns `null` for any other
         * name, including one without the prefix.
         */
        fun ofName(name: String): RecipientsOption? = byName[name]
    }
}

/**
 * Inheritance of contract options: method over service over default.
 *
 * An unset option (`null`), a timeout of `0` and [RecipientsOption.UNSPECIFIED] inherit. A `false` that is set on a
 * method overrides `persistent = true` on its service.
 */
internal object ProtocolResolution {

    /**
     * Resolves the service with the proto full name [protoName].
     */
    fun service(options: ServiceOptions, protoName: String): ResolvedService = ResolvedService(protoName, options)

    /**
     * Resolves the options of an rpc of [service].
     *
     * [emptyResponse] is whether the response type is `google.protobuf.Empty`. [streaming] is whether the request or
     * the response is streamed. The result is not validated, see [ResolvedMethod.problems].
     */
    fun method(
        options: MethodOptions,
        service: ResolvedService,
        emptyResponse: Boolean,
        streaming: Boolean,
    ): ResolvedMethod {
        val recipients = recipients(options.recipients, service.options.recipients)
        return ResolvedMethod(
            persistent = persistent(options.persistent, service.options.persistent),
            callTimeoutSeconds = callTimeoutSeconds(
                options.callTimeoutSeconds,
                service.options.callTimeoutSeconds
            ),
            recipients = recipients,
            resultKind = ResultKindOption.of(emptyResponse, recipients),
            streaming = streaming,
        )
    }

    /**
     * Returns the method value, else the service value, else `false`.
     */
    fun persistent(method: Boolean?, service: Boolean?): Boolean = method ?: service ?: false

    /**
     * Returns the explicit call timeout, or `null` when the default applies.
     *
     * A method value other than `0` wins over a service value other than `0`.
     */
    fun callTimeoutSeconds(method: Int?, service: Int?): Int? {
        return method?.takeIf { it != 0 } ?: service?.takeIf { it != 0 }
    }

    /**
     * Returns [callTimeoutSeconds], or the default for a persistent or non-persistent rpc when it is `null`.
     */
    fun effectiveCallTimeoutSeconds(callTimeoutSeconds: Int?, persistent: Boolean): Int {
        return callTimeoutSeconds
            ?: if (persistent) ProtocolDefaults.PERSISTENT_TIMEOUT_SECONDS else ProtocolDefaults.CORE_TIMEOUT_SECONDS
    }

    /**
     * Returns the explicit method value, else the explicit service value, else [RecipientsOption.ANY_INSTANCE].
     *
     * The result is never [RecipientsOption.UNSPECIFIED].
     */
    fun recipients(method: RecipientsOption?, service: RecipientsOption?): RecipientsOption {
        return method.explicit() ?: service.explicit() ?: RecipientsOption.ANY_INSTANCE
    }

    private fun RecipientsOption?.explicit(): RecipientsOption? {
        return takeIf { it != RecipientsOption.UNSPECIFIED }
    }
}
