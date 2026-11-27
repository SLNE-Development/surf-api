package dev.slne.surf.api.paper.server.packet

import com.github.retrooper.packetevents.PacketEvents
import dev.slne.surf.api.core.extensions.packetEvents
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.common.AbstractChannelInjector
import dev.slne.surf.api.paper.nms.common.NmsPacketListeners
import dev.slne.surf.api.paper.packet.listener.SurfPaperPacketListenerApi
import dev.slne.surf.api.paper.server.command.SuspendRequirementServiceImpl
import dev.slne.surf.api.paper.server.plugin
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder

@OptIn(NmsUseWithCaution::class)
object PacketApiLoader {

    fun onLoad() {
        setupPacketEvents()
    }

    @OptIn(NmsUseWithCaution::class)
    fun onEnable() {
        packetEvents.init()

        for (listener in NmsPacketListeners.instance.listeners) {
            SurfPaperPacketListenerApi.instance.registerListeners(listener)
        }

        SurfPaperPacketListenerApi.instance.registerListeners(
            SuspendRequirementServiceImpl.instance.getCommandSendPacketBlockerListener(),
        )

        AbstractChannelInjector.instance.register(plugin)
    }

    @OptIn(NmsUseWithCaution::class)
    fun onDisable() {
        packetEvents.terminate()

        for (listener in NmsPacketListeners.instance.listeners) {
            SurfPaperPacketListenerApi.instance.unregisterListeners(listener)
        }

        SurfPaperPacketListenerApi.instance.unregisterListeners(
            SuspendRequirementServiceImpl.instance.getCommandSendPacketBlockerListener(),
        )

        AbstractChannelInjector.instance.unregister()
    }

    private fun setupPacketEvents() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(plugin))
        packetEvents.load()
    }
}
