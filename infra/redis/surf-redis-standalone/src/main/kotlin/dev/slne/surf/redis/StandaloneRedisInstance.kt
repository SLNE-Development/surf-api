package dev.slne.surf.redis

import com.google.auto.service.AutoService
import java.nio.file.Path

@AutoService(RedisInstance::class)
class StandaloneRedisInstance(
    val name: String,
    configPath: Path,
) : RedisInstance() {
    override val dataPath = configPath
    override fun tryExtractPluginNameFromClass(clazz: Class<*>) = name

    fun create() {
        load()
    }

    fun shutdown() {
        disable()
    }
}