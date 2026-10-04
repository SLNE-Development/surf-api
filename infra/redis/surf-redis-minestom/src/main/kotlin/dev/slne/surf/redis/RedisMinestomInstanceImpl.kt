package dev.slne.surf.redis

import com.google.auto.service.AutoService
import java.nio.file.Path

@AutoService(RedisInstance::class)
class RedisMinestomInstanceImpl : RedisInstance() {
    override val dataPath: Path get() = RedisMinestomPlugin.dataDirectory

    override fun tryExtractPluginNameFromClass(clazz: Class<*>): String {
        return clazz.simpleName
    }
}