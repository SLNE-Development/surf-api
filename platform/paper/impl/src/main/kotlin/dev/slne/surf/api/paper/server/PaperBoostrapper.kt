package dev.slne.surf.api.paper.server

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.bootstrap.PluginProviderContext
import kotlinx.coroutines.runBlocking

@Suppress("unused", "UnstableApiUsage")
class PaperBoostrapper : PluginBootstrap {
    override fun bootstrap(bootstrapContext: BootstrapContext) {
        runBlocking {
            PaperInstance.bootstrap()
        }
    }

    override fun createPlugin(context: PluginProviderContext) = PaperMain()
}
