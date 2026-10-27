package dev.slne.surf.nats.js.codec

import dev.slne.surf.nats.js.external.nats.core.NatsPublishOptions
import js.typedarrays.Uint8Array

internal class OutgoingMessage(val headers: OutgoingHeaders, val body: Uint8Array<*>) {
    fun publishOptions(replyTo: String? = null): NatsPublishOptions {
        return NatsPublishOptions(reply = replyTo, headers = headers.msgHdrs)
    }
}
