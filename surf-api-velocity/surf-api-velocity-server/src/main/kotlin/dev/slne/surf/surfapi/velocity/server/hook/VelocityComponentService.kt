package dev.slne.surf.surfapi.velocity.server.hook

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.core.server.component.ComponentService
import dev.slne.surf.surfapi.velocity.server.velocityMain
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.io.File
import kotlin.jvm.optionals.getOrNull

@AutoService(ComponentService::class)
class VelocityComponentService : ComponentService() {

    override fun getClassloader(owner: Any): ClassLoader {
        return getInstanceFromOwner(owner).javaClass.classLoader
    }

    override fun isPluginLoaded(pluginId: String): Boolean {
        return velocityMain.server.pluginManager.isLoaded(pluginId)
    }

    override fun getLogger(owner: Any): ComponentLogger {
        return ComponentLogger.logger(getPluginContainerFromOwner(owner).description.id)
    }

    override fun getDataFolder(owner: Any): File {
        val pluginContainer = getPluginContainerFromOwner(owner)
        // Velocity plugins use @DataDirectory annotation to get data folder
        // We'll use the plugins directory with the plugin id as a fallback
        return File("plugins/${pluginContainer.description.id}")
    }

    private fun getPluginContainerFromOwner(owner: Any) =
        velocityMain.server.pluginManager.ensurePluginContainer(owner)

    private fun getInstanceFromOwner(owner: Any): Any {
        return getPluginContainerFromOwner(owner).instance.getOrNull()
            ?: error("Failed to get instance from owner: $owner")
    }
}