package dev.slne.surf.microservice.reliability.support

import dev.slne.surf.nats.rpc.NatsPersistentCall
import dev.slne.surf.nats.rpc.NatsPersistentReply
import java.security.MessageDigest
import java.util.HexFormat

/**
 * A delivery of a persistent message without NATS; [handler] stands in for the service method.
 */
class FakePersistentCall(
    override val messageId: String,
    request: ByteArray = messageId.encodeToByteArray(),
    override val deliveryCount: Long = 1,
    private val handler: suspend () -> NatsPersistentReply,
) : NatsPersistentCall {
    override val serviceName = "surf.reliability.test.v1.TestService"
    override val methodName = "Apply"
    override val requestFingerprint: String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(request))

    private var proceeded = false

    override suspend fun proceed(): NatsPersistentReply {
        check(!proceeded) { "proceed was already called for this delivery" }
        proceeded = true
        return handler()
    }

    companion object {
        /**
         * A reply carrying [text] as its value bytes.
         */
        fun valueReply(text: String): NatsPersistentReply =
            NatsPersistentReply.restore("surf-nats.value.v1", text.encodeToByteArray())
    }
}
