package dev.slne.surf.api.paper.server

import dev.slne.surf.api.core.server.CoreInstance
import dev.slne.surf.api.paper.server.listener.ListenerManager
import dev.slne.surf.api.paper.server.packet.PacketApiLoader
import dev.slne.surf.api.shared.internal.nms.NmsVersion
import dev.slne.surf.api.spring.surfBean
import org.bukkit.Bukkit

object PaperInstance : CoreInstance() {

    override suspend fun onLoad() {
        super.onLoad()

        PacketApiLoader.onLoad()
    }

    override suspend fun onEnable() {
        super.onEnable()

        val pluginVersion = plugin.pluginMeta.version
        val mcVersion = Bukkit.getMinecraftVersion()
        val nmsVersion: NmsVersion by surfBean()

        plugin.logger.info(
            "\u001B[36m\u001B[1mLoading surf-api v$pluginVersion for minecraft $mcVersion with nms $nmsVersion",
        )

        PacketApiLoader.onEnable()
        ListenerManager.registerListeners()
    }

    override suspend fun onDisable() {
        super.onDisable()

        ListenerManager.unregisterListeners()
        PacketApiLoader.onDisable()
    }
}