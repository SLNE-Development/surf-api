package dev.slne.surf.redis

import com.google.auto.service.AutoService
import dev.slne.minestom.lobby.api.plugin.MinestomPlugin
import dev.slne.minestom.lobby.api.plugin.annotation.MinestomPluginMeta

@AutoService(MinestomPlugin::class)
@MinestomPluginMeta(
    "surf-redis-minestom",
    dependsOn = ["surf-api-minestom"]
)
class RedisMinestomPlugin : MinestomPlugin(RedisMinestomEntrypoint::class.java)