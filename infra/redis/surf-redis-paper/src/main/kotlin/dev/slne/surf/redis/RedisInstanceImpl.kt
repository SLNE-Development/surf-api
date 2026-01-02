package dev.slne.surf.redis

import com.google.auto.service.AutoService

@AutoService(RedisInstance::class)
class RedisInstanceImpl : RedisInstance() {
    override val dataPath get() = plugin.dataPath
}