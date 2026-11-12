package dev.slne.surf.api.minestom.server.impl

import dev.slne.surf.api.core.server.CoreInstance
import dev.slne.surf.api.minestom.server.impl.inventory.framework.MinestomInventoryLoader

internal class SurfMinestomInstance(
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
