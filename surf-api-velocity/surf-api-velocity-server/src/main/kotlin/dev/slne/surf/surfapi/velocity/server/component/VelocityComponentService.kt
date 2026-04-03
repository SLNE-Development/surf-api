package dev.slne.surf.surfapi.velocity.server.component

import com.google.auto.service.AutoService
import com.velocitypowered.api.plugin.PluginContainer
import dev.slne.surf.surfapi.core.server.component.ComponentService
import dev.slne.surf.surfapi.velocity.server.plugin
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.io.path.Path

@AutoService(ComponentService::class)
class VelocityComponentService : ComponentService() {
    companion object {
        private val PLUGIN_PATH = Path("plugins")
    }

    override fun getClassloader(owner: Any): ClassLoader {
        ensureOwnerIsPluginContainer(owner)
        return owner.javaClass.classLoader
    }

    override fun isPluginLoaded(pluginId: String): Boolean {
        return plugin.server.pluginManager.isLoaded(pluginId)
    }

    override fun getLogger(owner: Any): ComponentLogger {
        ensureOwnerIsPluginContainer(owner)
        return ComponentLogger.logger(owner.description.id)
    }

    override fun getDataPath(owner: Any): Path {
        ensureOwnerIsPluginContainer(owner)
        return PLUGIN_PATH.resolve(owner.description.id)
    }

    @OptIn(ExperimentalContracts::class)
    private fun ensureOwnerIsPluginContainer(owner: Any): PluginContainer {
        contract {
            returns() implies (owner is PluginContainer)
        }

        return owner as? PluginContainer ?: error("Owner must be a PluginContainer")
    }
}