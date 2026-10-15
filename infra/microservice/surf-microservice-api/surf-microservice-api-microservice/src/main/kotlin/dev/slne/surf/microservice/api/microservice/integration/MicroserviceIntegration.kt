package dev.slne.surf.microservice.api.microservice.integration

import dev.slne.surf.microservice.api.common.util.InternalMicroserviceApi
import dev.slne.surf.microservice.api.microservice.Microservice

@InternalMicroserviceApi
interface MicroserviceIntegration {
    val name: String

    suspend fun start(microservice: Microservice)

    suspend fun stop(microservice: Microservice)
}
