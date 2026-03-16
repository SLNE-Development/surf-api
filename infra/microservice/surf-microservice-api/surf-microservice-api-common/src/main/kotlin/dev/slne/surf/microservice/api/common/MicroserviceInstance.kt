package dev.slne.surf.microservice.api.common

import dev.slne.surf.surfapi.core.api.util.requiredService

private val instance = requiredService<MicroserviceInstance>()

interface MicroserviceInstance {
    suspend fun onLoad()
    suspend fun onEnable()
    suspend fun onDisable()

    companion object : MicroserviceInstance by instance
}