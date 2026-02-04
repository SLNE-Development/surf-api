package dev.slne.surf.surfapi.bukkit.server.hook

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.bukkit.api.extensions.pluginManager
import dev.slne.surf.surfapi.bukkit.server.reflection.Reflection
import dev.slne.surf.surfapi.core.server.component.ComponentService
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.bukkit.plugin.java.JavaPlugin
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@AutoService(ComponentService::class)
class PaperComponentService : ComponentService() {

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

    override fun getDataPath(owner: Any): Path {
        ensureOwnerIsPlugin(owner)
        return owner.dataPath
    }

    @OptIn(ExperimentalContracts::class)
    private fun ensureOwnerIsPlugin(owner: Any): JavaPlugin {
        contract {
            returns() implies (owner is JavaPlugin)
        }

        return owner as? JavaPlugin ?: error("Owner must be a JavaPlugin")
    }
}