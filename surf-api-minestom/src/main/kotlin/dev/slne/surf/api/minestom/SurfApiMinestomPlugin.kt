package dev.slne.surf.api.minestom

import com.google.auto.service.AutoService
import dev.slne.minestom.lobby.api.plugin.MinestomPlugin
import dev.slne.minestom.lobby.api.plugin.annotation.MinestomPluginMeta
import dev.slne.surf.api.minestom.dialog.callback.DialogCallbackListener
import dev.slne.surf.api.minestom.impl.inventory.framework.MinestomInventoryLoader

@AutoService(MinestomPlugin::class)
@MinestomPluginMeta("surf-api-minestom")
internal class SurfApiMinestomPlugin : MinestomPlugin(SurfApiMinestomEntrypoint::class.java) {
    override fun configurePlugin() {
        bindEventRegistrar<MinestomInventoryLoader>()
        bindEventRegistrar<DialogCallbackListener>()
    }
}