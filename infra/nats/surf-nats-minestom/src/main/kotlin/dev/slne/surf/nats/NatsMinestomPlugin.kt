package dev.slne.surf.nats

import com.google.auto.service.AutoService
import dev.slne.minestom.lobby.api.plugin.MinestomPlugin
import dev.slne.minestom.lobby.api.plugin.annotation.MinestomPluginMeta

@Suppress("unused")
@AutoService(MinestomPlugin::class)
@MinestomPluginMeta("surf-nats-minestom", dependsOn = ["surf-api-minestom"])
@InternalNatsApi
class NatsMinestomPlugin : MinestomPlugin(NatsMinestomEntrypoint::class.java)