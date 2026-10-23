package dev.slne.surf.nats.protocol

/**
 * Values of a `(surf.nats.v1.event)` option; `null` is unset.
 */
internal class EventOptions(val subject: String? = null, val global: Boolean? = null)
