package dev.slne.surf.nats.protocol

/**
 * Values of the `surf.nats.v1.NatsRecipients` proto enum.
 *
 * Entry names are the proto constant names without the `NATS_RECIPIENTS_` prefix; [protoName] is the full constant name.
 */
internal enum class RecipientsOption(val number: Int, val protoName: String) {
    UNSPECIFIED(0, "NATS_RECIPIENTS_UNSPECIFIED"),
    ANY_INSTANCE(1, "NATS_RECIPIENTS_ANY_INSTANCE"),
    ALL_INSTANCES(2, "NATS_RECIPIENTS_ALL_INSTANCES");

    companion object {
        fun ofNumber(number: Int): RecipientsOption? = entries.firstOrNull { it.number == number }

        /**
         * Resolves a proto constant name such as `NATS_RECIPIENTS_ANY_INSTANCE`; names without the prefix return `null`.
         */
        fun ofName(name: String): RecipientsOption? = entries.firstOrNull { it.protoName == name }
    }
}

/**
 * Inheritance of contract options: method over service over default.
 *
 * An unset option (`null`), an empty name, a timeout of `0` and [RecipientsOption.UNSPECIFIED]
 * inherit. A `false` that is set on a method overrides `persistent = true` on its service.
 */
internal object ProtocolResolution {

    fun event(options: EventOptions, declaredName: String): ResolvedEvent {
        return ResolvedEvent(name(options.subject, declaredName), options.global ?: false)
    }

    fun service(options: ServiceOptions, declaredName: String): ResolvedService {
        return ResolvedService(name(options.name, declaredName), options.global ?: false, options)
    }

    fun method(
        options: MethodOptions,
        service: ResolvedService,
        declaredName: String,
        emptyResponse: Boolean,
        streaming: Boolean,
    ): ResolvedMethod {
        val recipients = recipients(options.recipients, service.options.recipients)
        return ResolvedMethod(
            name = name(options.name, declaredName),
            persistent = persistent(options.persistent, service.options.persistent),
            callTimeoutSeconds = callTimeoutSeconds(options.callTimeoutSeconds, service.options.callTimeoutSeconds),
            recipients = recipients,
            resultKind = ResultKindOption.of(emptyResponse, recipients),
            streaming = streaming,
        )
    }

    fun name(explicit: String?, declaredName: String): String {
        return if (explicit.isNullOrEmpty()) ProtocolNaming.kebabCase(declaredName) else explicit
    }

    fun persistent(method: Boolean?, service: Boolean?): Boolean = method ?: service ?: false

    /**
     * Returns the explicit call timeout, or `null` when the runtime default applies.
     */
    fun callTimeoutSeconds(method: Int?, service: Int?): Int? {
        return method?.takeIf { it != 0 } ?: service?.takeIf { it != 0 }
    }

    fun effectiveCallTimeoutSeconds(callTimeoutSeconds: Int?, persistent: Boolean): Int {
        return callTimeoutSeconds
            ?: if (persistent) ProtocolDefaults.PERSISTENT_TIMEOUT_SECONDS else ProtocolDefaults.CORE_TIMEOUT_SECONDS
    }

    /**
     * Returns [RecipientsOption.ANY_INSTANCE] or [RecipientsOption.ALL_INSTANCES], never the unspecified value.
     */
    fun recipients(method: RecipientsOption?, service: RecipientsOption?): RecipientsOption {
        return method.explicit() ?: service.explicit() ?: RecipientsOption.ANY_INSTANCE
    }

    private fun RecipientsOption?.explicit(): RecipientsOption? {
        return takeIf { it != RecipientsOption.UNSPECIFIED }
    }
}
