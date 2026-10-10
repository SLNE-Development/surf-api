package dev.slne.surf.nats.test.microservice.rpc

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.nats.rpc.natsPersistentMessageContext
import dev.slne.surf.nats.test.common.rpc.MicroserviceTestService
import java.util.concurrent.ConcurrentLinkedQueue

private val log = logger()

class MicroserviceTestServiceImpl : MicroserviceTestService {
    private val entries = ConcurrentLinkedQueue<String>()

    override suspend fun echo(text: String) = "echo: $text"

    override suspend fun fail(message: String): String = throw IllegalStateException(message)

    override suspend fun size(data: ByteArray) = data.size

    override suspend fun stored() = entries.toList()

    override suspend fun store(entry: String) {
        val context = natsPersistentMessageContext()
        log.atInfo()
            .log(
                "store '%s' (message %s, delivery %s)",
                entry, context.messageId, context.deliveryCount
            )
        entries += entry
    }

    override suspend fun storedCount() = entries.size
}
