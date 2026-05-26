package dev.slne.surf.redis

import com.google.auto.service.AutoService
import java.nio.file.Path

class StandaloneRedisInstance(
    val name: String,
    private val configPath: Path,
) {

    fun create() {
        InstanceImpl.pluginName = name
        InstanceImpl.pluginDataPath = configPath
        RedisInstance.instance.load()
    }

    fun shutdown() {
        RedisInstance.instance.disable()
    }

    @AutoService(RedisInstance::class)
    class InstanceImpl : RedisInstance() {

        companion object {
            lateinit var pluginName: String
            lateinit var pluginDataPath: Path
        }

        override val dataPath: Path
            get() = pluginDataPath

        override fun tryExtractPluginNameFromClass(clazz: Class<*>) =
            pluginName
    }
}