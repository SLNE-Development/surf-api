package dev.slne.surf.api.core.server.component

import com.google.auto.service.AutoService
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.kyori.adventure.util.Services
import java.nio.file.Path

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

    override fun getDataPath(owner: Any): Path {
        throwNotImplementedOnThisPlatform()
    }

    private fun throwNotImplementedOnThisPlatform(): Nothing {
        throw UnsupportedOperationException("This platform does not yet support components")
    }
}