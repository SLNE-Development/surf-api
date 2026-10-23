package dev.slne.surf.nats.test.microservice.rpc

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.nats.rpc.natsPersistentContext
import dev.slne.surf.nats.test.common.proto.v1.EchoResponse
import dev.slne.surf.nats.test.common.proto.v1.FailResponse
import dev.slne.surf.nats.test.common.proto.v1.MicroserviceTestService
import dev.slne.surf.nats.test.common.proto.v1.SizeResponse
import dev.slne.surf.nats.test.common.proto.v1.StoredCountResponse
import dev.slne.surf.nats.test.common.proto.v1.StoredResponse
import java.util.concurrent.ConcurrentLinkedQueue

private val log = logger()

class MicroserviceTestServiceImpl : MicroserviceTestService {
    private val entries = ConcurrentLinkedQueue<String>()

    override suspend fun echo(text: String) = EchoResponse(text = "echo: $text")

    override suspend fun fail(message: String): FailResponse = throw IllegalStateException(message)

    override suspend fun size(data: ByteArray) = SizeResponse(size = data.size)

    override suspend fun stored() = StoredResponse(entries = entries.toList())

    override suspend fun store(entry: String) {
        val context = natsPersistentContext()
        log.atInfo()
            .log(
                "store '%s' (message %s, delivery %s)",
                entry, context.messageId, context.deliveryCount
            )
        entries += entry
    }

    override suspend fun storedCount() = StoredCountResponse(count = entries.size)
}
