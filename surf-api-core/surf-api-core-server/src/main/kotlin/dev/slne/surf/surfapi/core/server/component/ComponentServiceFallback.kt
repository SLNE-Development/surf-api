package dev.slne.surf.surfapi.core.server.component

import com.google.auto.service.AutoService
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.kyori.adventure.util.Services
import java.io.File

@AutoService(ComponentService::class)
class ComponentServiceFallback : ComponentService(), Services.Fallback {

    override fun getClassloader(owner: Any): ClassLoader {
        throwNotImplementedOnThisPlatform()
    }

    override fun isPluginLoaded(pluginId: String): Boolean {
        throwNotImplementedOnThisPlatform()
    }

    override fun getLogger(owner: Any): ComponentLogger {
        throwNotImplementedOnThisPlatform()
    }

    override fun getDataFolder(owner: Any): File {
        throwNotImplementedOnThisPlatform()
    }

    private fun throwNotImplementedOnThisPlatform(): Nothing {
        throw UnsupportedOperationException("This platform does not yet support components")
    }
}