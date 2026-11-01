package dev.slne.surf.nats.test.microservice.rpc

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.nats.rpc.natsPersistentContext
import dev.slne.surf.nats.test.common.proto.v1.EchoRequest
import dev.slne.surf.nats.test.common.proto.v1.EchoResponse
import dev.slne.surf.nats.test.common.proto.v1.FailRequest
import dev.slne.surf.nats.test.common.proto.v1.FailResponse
import dev.slne.surf.nats.test.common.proto.v1.MicroserviceTestService
import dev.slne.surf.nats.test.common.proto.v1.SizeRequest
import dev.slne.surf.nats.test.common.proto.v1.SizeResponse
import dev.slne.surf.nats.test.common.proto.v1.StoreRequest
import dev.slne.surf.nats.test.common.proto.v1.StoredCountRequest
import dev.slne.surf.nats.test.common.proto.v1.StoredCountResponse
import dev.slne.surf.nats.test.common.proto.v1.StoredRequest
import dev.slne.surf.nats.test.common.proto.v1.StoredResponse
import java.util.concurrent.ConcurrentLinkedQueue

private val log = logger()

class MicroserviceTestServiceImpl : MicroserviceTestService {
    private val entries = ConcurrentLinkedQueue<String>()

    override suspend fun echo(request: EchoRequest) = EchoResponse(text = "echo: ${request.text}")

    override suspend fun fail(request: FailRequest): FailResponse = throw IllegalStateException(request.message)

    override suspend fun size(request: SizeRequest) = SizeResponse(size = request.data.size)

    override suspend fun stored(request: StoredRequest) = StoredResponse(entries = entries.toList())

    override suspend fun store(request: StoreRequest) {
        val entry = request.entry
        val context = natsPersistentContext()
        log.atInfo()
            .log(
                "store '%s' (message %s, delivery %s)",
                entry, context.messageId, context.deliveryCount
            )
        entries += entry
    }

    override suspend fun storedCount(request: StoredCountRequest) = StoredCountResponse(count = entries.size)
}
