package dev.slne.surf.microservice.api.microservice

import dev.slne.surf.microservice.api.common.util.InternalMicroserviceApi
import java.nio.file.Path

abstract class Microservice {
    abstract val dataPath: Path

    @InternalMicroserviceApi
    suspend fun bootstrap(args: List<String>) {
        onBootstrap(args)
    }

    @InternalMicroserviceApi
    suspend fun disable() {
        onDisable()
    }

    open suspend fun onBootstrap(args: List<String>) {}
    open suspend fun onDisable() {}

    companion object {
        @InternalMicroserviceApi
        lateinit var INSTANCE: Microservice
    }
}

inline fun <reified M : Microservice> getMicroservice() = Microservice.INSTANCE as M