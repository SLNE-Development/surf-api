package dev.slne.surf.nats.interop

import dev.slne.surf.nats.codec.NatsCodec
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.codec.header
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.protocol.ServiceNames
import dev.slne.surf.nats.test.proto.v1.InteropVectorRequest
import dev.slne.surf.nats.test.proto.v1.InteropVectorService
import io.nats.client.Dispatcher

/**
 * Answers `InteropVectorService` calls for [owner] with the bytes of raw vectors, which the generated responder cannot
 * produce: a Kotlin message of another type than the JS-facing response.
 */
internal class RawVectorResponder(private val owner: String, private val book: VectorBook) : AutoCloseable {
    private val connection = NatsConnectionManager.connection()
    private val dispatcher: Dispatcher = connection.createDispatcher()

    init {
        val descriptor = InteropVectorService.NATS_DESCRIPTOR
        val names = ServiceNames(owner, descriptor.name, descriptor.majorVersion)
        dispatcher.subscribe("${names.rpcBase}.*") { request ->
            val body = try {
                val name = NatsCodec.decode(
                    InteropVectorRequest.serializer(),
                    request.data,
                    request.header(ProtocolHeaders.CONTENT_TYPE),
                ).name
                val vector = book.serve(name)
                check(vector.raw) { "Vector $name is served by the generated responder" }
                vector.encode()
            } catch (e: Exception) {
                val headers = NatsHeaders.base(owner).add(ProtocolHeaders.ERROR, e.javaClass.name)
                connection.publish(request.replyTo, headers, e.message.orEmpty().encodeToByteArray())
                return@subscribe
            }
            connection.publish(request.replyTo, NatsHeaders.base(owner), body)
        }
    }

    override fun close() {
        connection.closeDispatcher(dispatcher)
    }
}
