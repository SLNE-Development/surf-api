package dev.slne.surf.redis

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import java.nio.file.Path

@Suppress("UnstableApiUsage")
class PaperBootstrap : PluginBootstrap {
    override fun bootstrap(context: BootstrapContext) {
        dataPath = context.dataDirectory

        RedisInstance.instance.load()
    }

    companion object {
        lateinit var dataPath: Path
    }
}