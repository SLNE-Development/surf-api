package dev.slne.surf.api.paper.server

import dev.slne.surf.api.core.server.messages.SurfAdventure5AbiPatcher
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.bootstrap.PluginProviderContext
import kotlinx.coroutines.runBlocking

@Suppress("unused", "UnstableApiUsage")
class PaperBoostrapper : PluginBootstrap {
    override fun bootstrap(bootstrapContext: BootstrapContext) {
        SurfAdventure5AbiPatcher.patchPluginsDirectory(
            pluginsDir = bootstrapContext.pluginSource.parent,
            log = { message ->
                bootstrapContext.logger.info("[Adventure5AbiPatcher] $message")
            },
            warn = { message ->
                bootstrapContext.logger.warn("[Adventure5AbiPatcher] $message")
            },
        )

        runBlocking {
            PaperInstance.bootstrap()
        }
    }

    override fun createPlugin(context: PluginProviderContext) = PaperMain()
}
