package dev.slne.surf.surfapi.bukkit.server

import dev.slne.surf.surfapi.bukkit.api.SurfApiBukkit
import dev.slne.surf.surfapi.bukkit.server.impl.SurfApiBukkitImpl
import dev.slne.surf.surfapi.bukkit.server.inventory.framework.InventoryLoader
import dev.slne.surf.surfapi.bukkit.server.listener.ListenerManager
import dev.slne.surf.surfapi.bukkit.server.packet.PacketApiLoader
import dev.slne.surf.surfapi.bukkit.server.reflection.Reflection
import dev.slne.surf.surfapi.core.server.CoreInstance

object BukkitInstance : CoreInstance() {

    override suspend fun onLoad() {
        super.onLoad()

        initObjects()
        PacketApiLoader.onLoad()
        InventoryLoader.load()
    }

    override suspend fun onEnable() {
        super.onEnable()

        PacketApiLoader.onEnable()
        InventoryLoader.enable()
        ListenerManager.registerListeners()
        (SurfApiBukkit.INSTANCE as SurfApiBukkitImpl).onEnable()
    }

    override suspend fun onDisable() {
        super.onDisable()

        ListenerManager.unregisterListeners()
        PacketApiLoader.onDisable()
        InventoryLoader.disable()
    }

    private fun initObjects() {
        Reflection
    }
}