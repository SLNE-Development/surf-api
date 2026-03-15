package dev.slne.surf.microservice.test.core.client

import dev.slne.surf.rabbitmq.api.ClientRabbitMQApi
import dev.slne.surf.surfapi.core.api.util.requiredService

val clientInstance = requiredService<MessageClientInstance>()

abstract class MessageClientInstance {
    lateinit var rabbitApi: ClientRabbitMQApi
        private set

    suspend fun onLoad() {
        rabbitApi = ClientRabbitMQApi.create(1, "surf-test")
        rabbitApi.freezeAndConnect()
    }

    suspend fun onEnable() {

    }

    suspend fun onDisable() {

    }
}