package dev.slne.surf.redis

import com.google.auto.service.AutoService

@AutoService(RedisInstance::class)
class VelocityRedisInstanceImpl : RedisInstance() {
    override val dataPath get() = plugin.dataPath
}