package dev.slne.surf.api.paper.server

import dev.slne.surf.api.core.server.CoreInstance
import dev.slne.surf.api.paper.SurfApiPaper
import dev.slne.surf.api.paper.server.display.DisplayLoader
import dev.slne.surf.api.paper.server.impl.SurfApiPaperImpl
import dev.slne.surf.api.paper.server.inventory.framework.InventoryLoader
import dev.slne.surf.api.paper.server.listener.ListenerManager
import dev.slne.surf.api.paper.server.packet.PacketApiLoader
import dev.slne.surf.api.paper.server.reflection.Reflection

object PaperInstance : CoreInstance() {

    override suspend fun onLoad() {
        super.onLoad()

        initObjects()
        PacketApiLoader.onLoad()
        InventoryLoader.load()
    }

    override suspend fun onEnable() {
        super.onEnable()

        PacketApiLoader.onEnable()
        DisplayLoader.onEnable()
        InventoryLoader.enable()
        ListenerManager.registerListeners()
        (SurfApiPaper.INSTANCE as SurfApiPaperImpl).onEnable()
    }

    override suspend fun onDisable() {
        super.onDisable()

        DisplayLoader.onDisable()
        ListenerManager.unregisterListeners()
        PacketApiLoader.onDisable()
        InventoryLoader.disable()
    }

    private fun initObjects() {
        Reflection
    }
}