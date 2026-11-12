package dev.slne.surf.microservice.runtime.lifecycle

import dev.slne.surf.api.standalone.SurfApiStandaloneBootstrap

/**
 * Boots and enables surf-api without its own shutdown hook. The launcher's hook stops it in order.
 */
internal object SurfApiComponent : RuntimeComponent {
    override val name = "surf-api"

    override suspend fun start() {
        SurfApiStandaloneBootstrap.bootstrap()
        SurfApiStandaloneBootstrap.enable(registerShutdownHook = false)
    }

    override suspend fun stop() {
        SurfApiStandaloneBootstrap.shutdown()
    }
}
