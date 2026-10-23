package dev.slne.surf.nats.protocol

/**
 * Values of a `(surf.nats.v1.method)` option; `null` is unset.
 */
internal class MethodOptions(
    val name: String? = null,
    val persistent: Boolean? = null,
    val callTimeoutSeconds: Int? = null,
    val recipients: RecipientsOption? = null,
)
