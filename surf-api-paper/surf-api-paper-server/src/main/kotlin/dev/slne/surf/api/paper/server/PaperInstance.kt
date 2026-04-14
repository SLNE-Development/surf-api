package dev.slne.surf.api.paper.server

import dev.slne.surf.api.core.server.CoreInstance
import dev.slne.surf.api.paper.SurfApiPaper
import dev.slne.surf.api.paper.nms.common.NmsProvider
import dev.slne.surf.api.paper.server.impl.SurfApiPaperImpl
import dev.slne.surf.api.paper.server.inventory.framework.InventoryLoader
import dev.slne.surf.api.paper.server.listener.ListenerManager
import dev.slne.surf.api.paper.server.packet.PacketApiLoader
import org.bukkit.Bukkit

object PaperInstance : CoreInstance() {

    override suspend fun onLoad() {
        super.onLoad()

        PacketApiLoader.onLoad()
        InventoryLoader.load()
    }

    override suspend fun onEnable() {
        super.onEnable()

        val pluginVersion = plugin.pluginMeta.version
        val mcVersion = Bukkit.getMinecraftVersion()
        val nmsVersion = NmsProvider.current.version

        plugin.logger.info("\u001B[36m\u001B[1mLoading surf-api v$pluginVersion for minecraft $mcVersion with nms $nmsVersion")

        PacketApiLoader.onEnable()
        InventoryLoader.enable()
        ListenerManager.registerListeners()
        (SurfApiPaper.INSTANCE as SurfApiPaperImpl).onEnable()
    }

    override suspend fun onDisable() {
        super.onDisable()

        ListenerManager.unregisterListeners()
        PacketApiLoader.onDisable()
        InventoryLoader.disable()
    }
}