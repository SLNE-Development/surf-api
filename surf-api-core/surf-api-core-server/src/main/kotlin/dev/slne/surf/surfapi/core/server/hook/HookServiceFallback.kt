package dev.slne.surf.surfapi.core.server.hook

import com.google.auto.service.AutoService
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.kyori.adventure.util.Services
import java.io.InputStream

@AutoService(HookService::class)
class HookServiceFallback : HookService(), Services.Fallback {
    override fun readHooksFileFromResources(owner: Any, fileName: String): InputStream? {
        throwNotImplementedOnThisPlatform()
    }

    override fun getClassloader(owner: Any): ClassLoader {
        throwNotImplementedOnThisPlatform()
    }

    override fun isPluginLoaded(pluginId: String): Boolean {
        throwNotImplementedOnThisPlatform()
    }

    override fun getLogger(owner: Any): ComponentLogger {
        throwNotImplementedOnThisPlatform()
    }

    private fun throwNotImplementedOnThisPlatform(): Nothing {
        throw UnsupportedOperationException("This platform does not yet support hooks")
    }
}