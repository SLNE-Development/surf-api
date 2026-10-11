package dev.slne.surf.nats.test.microservice

import com.google.auto.service.AutoService
import dev.slne.surf.nats.registerService
import dev.slne.surf.nats.test.common.NatsService
import dev.slne.surf.nats.test.common.rpc.MicroserviceTestService
import dev.slne.surf.nats.test.microservice.event.ChatListener
import dev.slne.surf.nats.test.microservice.rpc.MicroserviceTestServiceImpl

@AutoService(NatsService::class)
class MicroserviceNatsServiceImpl : NatsService() {

    override suspend fun start() {
        super.start()

        client.registerListener(ChatListener)
        client.registerService<MicroserviceTestService>(MicroserviceTestServiceImpl())
    }
}