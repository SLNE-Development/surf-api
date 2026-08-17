package dev.slne.surf.api.minestom.impl

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.surf.api.core.server.CoreInstance
import dev.slne.surf.api.minestom.impl.inventory.framework.MinestomInventoryLoader

@Singleton
internal class SurfMinestomInstance @Inject constructor(
    private val inventoryLoader: MinestomInventoryLoader
) : CoreInstance() {

    override suspend fun onEnable() {
        super.onEnable()
        inventoryLoader.enable()
    }

    override suspend fun onDisable() {
        super.onDisable()
        inventoryLoader.disable()
    }
}