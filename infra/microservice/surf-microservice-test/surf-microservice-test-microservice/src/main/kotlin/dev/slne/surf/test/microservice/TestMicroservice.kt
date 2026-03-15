package dev.slne.surf.test.microservice

import com.google.auto.service.AutoService
import dev.slne.surf.microservice.api.Microservice
import dev.slne.surf.rabbitmq.api.ServerRabbitMQApi
import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.test.microservice.handler.TestRabbitHandler

@AutoService(Microservice::class)
class TestMicroservice : Microservice() {
    private val serverApi = ServerRabbitMQApi.create(1, "surf-test")
    private val log = logger()

    override suspend fun onBootstrap(args: List<String>) {
        log.atInfo().log("Running onBootstrap")

        serverApi.registerRequestHandler(TestRabbitHandler)
        serverApi.freezeAndConnect()
        testCommand()
    }

    override suspend fun onDisable() {
        log.atInfo().log("Running onDisable")
    }
}