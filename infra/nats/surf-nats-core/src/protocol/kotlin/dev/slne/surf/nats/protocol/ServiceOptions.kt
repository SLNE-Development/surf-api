package dev.slne.surf.nats.protocol

/**
 * Values of a `(surf.nats.v1.service)` option; `null` is unset.
 */
internal class ServiceOptions(
    val name: String? = null,
    val global: Boolean? = null,
    val callTimeoutSeconds: Int? = null,
    val recipients: RecipientsOption? = null,
    val persistent: Boolean? = null,
)
