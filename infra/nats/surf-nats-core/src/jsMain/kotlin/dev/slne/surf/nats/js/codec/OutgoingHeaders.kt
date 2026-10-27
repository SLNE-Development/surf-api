package dev.slne.surf.nats.js.codec

import dev.slne.surf.nats.js.external.nats.core.MsgHdrs

/**
 * Headers of an outgoing message with their encoded size in [bytes], counted like `MsgHdrs.encode()`.
 *
 * [set] adds a header that is not set yet; replacing a value would count both lines.
 */
internal class OutgoingHeaders(val msgHdrs: MsgHdrs, bytes: Int) {
    var bytes: Int = bytes
        private set

    fun set(name: String, value: String) {
        msgHdrs.set(name, value)
        bytes += NatsHeaders.lineSize(name, value)
    }
}
