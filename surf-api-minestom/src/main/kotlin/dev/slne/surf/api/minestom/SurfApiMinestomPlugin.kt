package dev.slne.surf.api.minestom

import com.google.auto.service.AutoService
import dev.slne.minestom.lobby.api.plugin.MinestomPlugin
import dev.slne.minestom.lobby.api.plugin.annotation.MinestomPluginMeta

@AutoService(MinestomPlugin::class)
@MinestomPluginMeta("surf-api-minestom")
internal class SurfApiMinestomPlugin: MinestomPlugin(SurfApiMinestomEntrypoint::class.java)