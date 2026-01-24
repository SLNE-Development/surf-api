package dev.slne.surf.surfapi.bukkit.server.hook

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.bukkit.api.extensions.pluginManager
import dev.slne.surf.surfapi.bukkit.server.reflection.Reflection
import dev.slne.surf.surfapi.core.server.hook.HookService
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.bukkit.plugin.java.JavaPlugin
import java.io.InputStream
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@AutoService(HookService::class)
class PaperHookService : HookService() {
    override fun readHooksFileFromResources(owner: Any, fileName: String): InputStream? {
        ensureOwnerIsPlugin(owner)
        return owner.getResource(fileName)
    }

    override fun getClassloader(owner: Any): ClassLoader {
        ensureOwnerIsPlugin(owner)
        return Reflection.JAVA_PLUGIN_PROXY.getClassLoader(owner)
    }

    override fun isPluginLoaded(pluginId: String): Boolean {
        return pluginManager.getPlugin(pluginId) != null
    }

    override fun getLogger(owner: Any): ComponentLogger {
        ensureOwnerIsPlugin(owner)
        return owner.componentLogger
    }

    @OptIn(ExperimentalContracts::class)
    private fun ensureOwnerIsPlugin(owner: Any): JavaPlugin {
        contract {
            returns() implies (owner is JavaPlugin)
        }

        return owner as? JavaPlugin ?: error("Owner must be a JavaPlugin")
    }
}