package dev.slne.surf.nats.protocol

/**
 * Values of a `(surf.nats.v1.service)` option; `null` is unset.
 */
internal class ServiceOptions(
    val callTimeoutSeconds: Int? = null,
    val recipients: RecipientsOption? = null,
    val persistent: Boolean? = null,
)
