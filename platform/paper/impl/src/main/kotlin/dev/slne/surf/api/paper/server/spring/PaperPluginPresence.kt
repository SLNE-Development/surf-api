package dev.slne.surf.api.paper.server.spring

import dev.slne.surf.api.spring.PluginPresence
import io.papermc.paper.plugin.configuration.PluginMeta
import org.bukkit.Bukkit

/**
 * Answers plugin presence on Paper, including during the bootstrap phase.
 *
 * Once the server exists, Bukkit's plugin manager answers. Before that, in the bootstrap phase,
 * Paper exposes no API for it, so the names come from Paper's plugin provider storage, read
 * reflectively. Both include the names a plugin declares under `provides`.
 */
internal object PaperPluginPresence : PluginPresence {

    private val registeredNames: Set<String> by lazy(::readRegisteredPluginNames)

    override fun isInstalled(name: String): Boolean {
        val server = runCatching { Bukkit.getServer() }.getOrNull()
        if (server != null) return server.pluginManager.getPlugin(name) != null

        return name in registeredNames
    }

    private fun readRegisteredPluginNames(): Set<String> {
        val loader = PluginMeta::class.java.classLoader
        val handlerType = Class.forName(
            "io.papermc.paper.plugin.entrypoint.LaunchEntryPointHandler",
            true,
            loader,
        )
        val entrypointType = Class.forName(
            "io.papermc.paper.plugin.entrypoint.Entrypoint",
            true,
            loader,
        )
        val storageType = Class.forName(
            "io.papermc.paper.plugin.storage.ProviderStorage",
            true,
            loader,
        )
        val providerType = Class.forName(
            "io.papermc.paper.plugin.provider.PluginProvider",
            true,
            loader,
        )

        val handler = handlerType.getField("INSTANCE").get(null)
        val pluginEntrypoint = entrypointType.getField("PLUGIN").get(null)
        val storage = handlerType.getMethod("get", entrypointType).invoke(handler, pluginEntrypoint)
        val providers = storageType.getMethod("getRegisteredProviders")
            .invoke(storage) as Iterable<*>
        val getMeta = providerType.getMethod("getMeta")

        return providers
            .map { provider -> getMeta.invoke(provider) as PluginMeta }
            .flatMapTo(HashSet()) { meta -> listOf(meta.name) + meta.providedPlugins }
    }
}
