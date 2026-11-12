package dev.slne.surf.nats.protocol

/**
 * Values of a `(surf.nats.v1.service)` option as declared on one service.
 *
 * A `null` property is unset and falls back to the protocol default, see [ProtocolResolution].
 */
internal class ServiceOptions(
    val callTimeoutSeconds: Int? = null,
    val recipients: RecipientsOption? = null,
    val persistent: Boolean? = null,
)
