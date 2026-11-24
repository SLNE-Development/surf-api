package dev.slne.surf.api.paper.server

import dev.slne.surf.api.nats.NatsPlatform
import dev.slne.surf.api.paper.plugin.SurfPaperPluginRuntime
import dev.slne.surf.api.paper.server.spring.PaperSpringRuntime
import dev.slne.surf.api.redis.RedisPlatform
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.bootstrap.PluginProviderContext
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader
import kotlinx.coroutines.runBlocking

@Suppress("unused", "UnstableApiUsage")
class PaperBoostrapper : PluginBootstrap {

    override fun bootstrap(bootstrapContext: BootstrapContext) {
        NatsPlatform.install(bootstrapContext.dataDirectory)
        RedisPlatform.install(bootstrapContext.dataDirectory) { type ->
            (type.classLoader as? ConfiguredPluginClassLoader)?.configuration?.name
                ?: type.simpleName
        }

        springRuntime = PaperSpringRuntime(PaperBoostrapper::class.java.classLoader)
        SurfPaperPluginRuntime.install(springRuntime)

        runBlocking {
            springRuntime.bootstrapRoot()
            PaperInstance.bootstrap()
        }
    }

    override fun createPlugin(context: PluginProviderContext): PaperMain = PaperMain()

    companion object {

        /**
         * surf-api's Spring runtime, created in the bootstrap phase and shut down by [PaperMain].
         */
        internal lateinit var springRuntime: PaperSpringRuntime
            private set
    }
}
