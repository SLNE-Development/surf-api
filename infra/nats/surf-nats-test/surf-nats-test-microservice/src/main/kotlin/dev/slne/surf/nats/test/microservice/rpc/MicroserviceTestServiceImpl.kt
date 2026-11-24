package dev.slne.surf.nats.test.microservice.rpc

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.nats.NatsService
import dev.slne.surf.nats.rpc.natsPersistentContext
import dev.slne.surf.nats.test.common.proto.v1.*
import java.util.concurrent.ConcurrentLinkedQueue

private val log = logger()

/**
 * Serves [MicroserviceTestService] and keeps the entries of `store` in memory.
 */
@NatsService
class MicroserviceTestServiceImpl : MicroserviceTestService {
    private val entries = ConcurrentLinkedQueue<String>()

    override suspend fun echo(request: EchoRequest): EchoResponse {
        return EchoResponse(text = "echo: ${request.text}")
    }

    override suspend fun fail(request: FailRequest): FailResponse {
        return throw IllegalStateException(request.message)
    }

    override suspend fun size(request: SizeRequest): SizeResponse {
        return SizeResponse(size = request.data.size)
    }

    override suspend fun stored(request: StoredRequest): StoredResponse {
        return StoredResponse(entries = entries.toList())
    }

    override suspend fun store(request: StoreRequest) {
        val entry = request.entry
        val context = natsPersistentContext()
        log.atInfo()
            .log(
                "store '%s' (message %s, delivery %s)",
                entry, context.messageId, context.deliveryCount,
            )
        entries += entry
    }

    override suspend fun storedCount(request: StoredCountRequest): StoredCountResponse {
        return StoredCountResponse(count = entries.size)
    }
}
