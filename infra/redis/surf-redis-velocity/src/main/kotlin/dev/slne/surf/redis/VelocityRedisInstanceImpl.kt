package dev.slne.surf.redis

import com.google.auto.service.AutoService
import dev.slne.surf.redis.reflection.JavaPluginLoaderProxy
import dev.slne.surf.redis.reflection.SerializedPluginDescriptionProxy
import dev.slne.surf.surfapi.core.api.util.logger
import kotlin.io.path.Path
import kotlin.io.path.toPath
import kotlin.jvm.optionals.getOrNull

@AutoService(RedisInstance::class)
class VelocityRedisInstanceImpl : RedisInstance() {
    private val log = logger()
    override val dataPath get() = plugin.dataPath

    private val velocityPluginLoader by lazy {
        JavaPluginLoaderProxy.get().createInstance(plugin.proxy, Path("plugins"))
    }

    override fun tryExtractPluginNameFromClass(clazz: Class<*>): String {
        try {
            val jarLocation = clazz.protectionDomain.codeSource.location.toURI().toPath()
            val pluginInfo = JavaPluginLoaderProxy.get()
                .getSerializedPluginInfo(velocityPluginLoader, jarLocation)
                .getOrNull()

            if (pluginInfo == null) {
                log.atWarning()
                    .log("Could not extract plugin info for class ${clazz.name}, falling back to simple name")
                return clazz.simpleName
            }

            val id = SerializedPluginDescriptionProxy.get().getId(pluginInfo)
            return id
        } catch (e: Exception) {
            log.atWarning()
                .withCause(e)
                .log("Could not extract plugin info for class ${clazz.name}, falling back to simple name")
        }

        return clazz.simpleName
    }
}