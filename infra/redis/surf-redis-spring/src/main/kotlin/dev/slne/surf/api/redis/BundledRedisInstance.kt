package dev.slne.surf.api.redis

import dev.slne.surf.redis.RedisInstance
import java.nio.file.Path

/**
 * The [RedisInstance] of the platforms that bundle Redis into surf-api.
 *
 * Reads everything platform specific from [RedisPlatform]. The configuration lives in the `redis`
 * folder of surf-api's data folder; the first access copies an existing configuration of the old
 * platform plugins there, see [RedisConfigLocation].
 */
internal class BundledRedisInstance : RedisInstance() {

    override val dataPath: Path by lazy { RedisConfigLocation.resolve(RedisPlatform.dataDirectory) }

    override fun tryExtractPluginNameFromClass(clazz: Class<*>): String {
        return RedisPlatform.pluginNameOf(clazz)
    }
}
