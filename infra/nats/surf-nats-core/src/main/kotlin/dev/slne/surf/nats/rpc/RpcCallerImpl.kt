package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.*
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.connection.NatsConnectionManager
import io.nats.client.JetStreamStatusException
import io.nats.client.Message
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.future.await
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.toJavaDuration

@InternalNatsApi
internal class RpcCallerImpl(
    private val client: NatsClientImpl,
    private val service: ServiceDescriptor,
) : RpcCaller {
    override suspend fun call(index: Int, args: Array<Any?>): Any? {
        val method = service.methods[index]
        if (method.responders != ResponderMode.ONE) {
            TODO("Not yet implemented")
        }

        val timeout = currentCoroutineContext()[NatsTimeoutElement]?.timeout
            ?: method.timeout
            ?: RpcDefaults.CORE_TIMEOUT
        val headers = NatsHeaders.base(client.name, WireFormat.CBOR)
            .add(
                NatsHeaders.DEADLINE,
                (System.currentTimeMillis() + timeout.inWholeMilliseconds).toString()
            )
        val body = client.codec.encode(method.arguments, args, WireFormat.CBOR)

        val reply = try {
            NatsConnectionManager.connection()
                .requestWithTimeout(method.subject, headers, body, timeout.toJavaDuration())
                .await()
        } catch (_: JetStreamStatusException) {
            throw NatsNoRespondersException(method.subject)
        } catch (_: CancellationException) {
            currentCoroutineContext().ensureActive()
            throw NatsRequestTimeoutException(method.subject, timeout)
        }

        return decodeReply(method, reply)
    }

    private fun decodeReply(
        method: MethodDescriptor,
        reply: Message
    ): Any? {
        val headers = if (reply.hasHeaders()) reply.headers else null

        headers?.getFirst(NatsHeaders.ERROR)?.let { remoteType ->
            throw NatsRemoteException(remoteType, reply.data.decodeToString())
        }

        val contentType = headers?.getFirst(NatsHeaders.CONTENT_TYPE)

        return when (val result = method.result) {
            ResultKind.Unit -> Unit
            is ResultKind.Value -> client.codec.decode(result.serializer, reply.data, contentType)
            is ResultKind.Answer -> {
                if (headers?.getFirst(NatsHeaders.ANSWER) == NatsHeaders.ANSWER_NONE) {
                    Answer.None
                } else {
                    Answer.Of(client.codec.decode(result.serializer, reply.data, contentType))
                }
            }
        }
    }
}