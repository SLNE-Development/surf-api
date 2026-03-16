package dev.slne.surf.microservice.api.microservice

import dev.slne.surf.microservice.api.util.InternalMicroserviceApi

abstract class Microservice {
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

val microservice get() = Microservice.INSTANCE