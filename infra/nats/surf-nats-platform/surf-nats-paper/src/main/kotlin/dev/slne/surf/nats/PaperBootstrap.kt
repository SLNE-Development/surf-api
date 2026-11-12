package dev.slne.surf.nats

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import kotlinx.coroutines.runBlocking

/**
 * Connects NATS in the Paper bootstrap phase with the configuration in the plugin data directory.
 *
 * Blocks the bootstrap thread until the connection is established.
 */
@Suppress("UnstableApiUsage")
@InternalNatsApi
class PaperBootstrap: PluginBootstrap {
    override fun bootstrap(context: BootstrapContext) {
        PaperNatsInstance.get().dataPath = context.dataDirectory

        runBlocking {
            PaperNatsInstance.get().load()
        }
    }
}