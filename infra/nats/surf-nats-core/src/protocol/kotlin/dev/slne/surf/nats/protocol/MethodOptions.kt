package dev.slne.surf.nats.protocol

/**
 * Values of a `(surf.nats.v1.method)` option as declared on one rpc.
 *
 * A `null` property is unset and inherits the service value, see [ProtocolResolution].
 */
internal class MethodOptions(
    val persistent: Boolean? = null,
    val callTimeoutSeconds: Int? = null,
    val recipients: RecipientsOption? = null,
)
