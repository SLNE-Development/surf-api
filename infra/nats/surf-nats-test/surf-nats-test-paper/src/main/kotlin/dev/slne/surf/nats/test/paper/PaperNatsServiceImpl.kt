package dev.slne.surf.nats.test.paper

import com.google.auto.service.AutoService
import dev.slne.surf.nats.registerService
import dev.slne.surf.nats.test.common.NatsService
import dev.slne.surf.nats.test.common.proto.v1.PaperTestService
import dev.slne.surf.nats.test.paper.event.PaperChatListener
import dev.slne.surf.nats.test.paper.rpc.PaperTestServiceImpl

@AutoService(NatsService::class)
class PaperNatsServiceImpl : NatsService() {

    override suspend fun start() {
        super.start()

        client.registerListener(PaperChatListener(plugin))
        client.registerService<PaperTestService>(PaperTestServiceImpl(plugin))
    }
}