package dev.slne.surf.microservice.api

import com.google.common.flogger.FluentLogger
import dev.slne.surf.microservice.api.util.InternalMicroserviceApi
import dev.slne.surf.surfapi.standalone.SurfApiStandaloneBootstrap

abstract class Microservice {
    abstract val logger: FluentLogger

    @InternalMicroserviceApi
    suspend fun bootstrap(args: List<String>) {
        SurfApiStandaloneBootstrap.bootstrap()
        SurfApiStandaloneBootstrap.enable()

        onBootstrap(args)
    }

    @InternalMicroserviceApi
    suspend fun disable() {
        onDisable()

        SurfApiStandaloneBootstrap.shutdown()
    }

    open suspend fun onBootstrap(args: List<String>) {}
    open suspend fun onDisable() {}

    companion object {
        @InternalMicroserviceApi
        lateinit var INSTANCE: Microservice
            internal set
    }
}

val microservice get() = Microservice.INSTANCE
val log get() = microservice.logger