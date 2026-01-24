package dev.slne.surf.surfapi.velocity.server.hook

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.core.server.hook.HookService
import dev.slne.surf.surfapi.velocity.server.velocityMain
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.io.IOException
import java.io.InputStream
import kotlin.jvm.optionals.getOrNull

@AutoService(HookService::class)
class VelocityHookService : HookService() {
    override fun readHooksFileFromResources(owner: Any, fileName: String): InputStream? {
        return try {
            val url = getClassloader(owner).getResource(fileName) ?: return null
            val connection = url.openConnection()
            connection.useCaches = false
            connection.getInputStream()
        } catch (_: IOException) {
            null
        }
    }

    override fun getClassloader(owner: Any): ClassLoader {
        return getInstanceFromOwner(owner).javaClass.classLoader
    }

    override fun isPluginLoaded(pluginId: String): Boolean {
        return velocityMain.server.pluginManager.isLoaded(pluginId)
    }

    override fun getLogger(owner: Any): ComponentLogger {
        return ComponentLogger.logger(getPluginContainerFromOwner(owner).description.id)
    }

    private fun getPluginContainerFromOwner(owner: Any) =
        velocityMain.server.pluginManager.ensurePluginContainer(owner)

    private fun getInstanceFromOwner(owner: Any): Any {
        return getPluginContainerFromOwner(owner).instance.getOrNull()
            ?: error("Failed to get instance from owner: $owner")
    }
}