package dev.slne.surf.nats.rpc

import com.google.common.hash.Hashing
import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.bytecode.ServiceInvoker
import dev.slne.surf.nats.bytecode.call
import java.util.concurrent.atomic.AtomicBoolean

/**
 * One delivery of a persistent call as seen by a [NatsPersistentInterceptor].
 *
 * [proceed] runs the handler at most once and throws [IllegalStateException] on a second call. The request
 * fingerprint is the SHA-256 hash of the encoded request, computed on first access.
 */
@InternalNatsApi
internal class PersistentCall(
    override val serviceName: String,
    private val method: MethodDescriptor,
    private val invoker: ServiceInvoker,
    private val request: Any,
    private val requestBytes: ByteArray,
    override val messageId: String,
    override val deliveryCount: Long,
) : NatsPersistentCall {

    private val proceeded = AtomicBoolean()

    override val methodName: String get() = method.generated.protoName

    override val requestFingerprint: String by lazy {
        Hashing.sha256().hashBytes(requestBytes).toString()
    }

    override suspend fun proceed(): NatsPersistentReply {
        check(proceeded.compareAndSet(false, true)) {
            "proceed() was already called for ${method.label}"
        }

        return method.encodeReply(invoker.call(request))
    }
}
