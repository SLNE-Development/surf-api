package dev.slne.surf.api.paper.server.nms.v26_2

import dev.slne.surf.api.paper.glow.SurfGlowingApi
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.*
import dev.slne.surf.api.paper.nms.bridges.packets.SurfPaperNmsPacketBridges
import dev.slne.surf.api.paper.nms.bridges.packets.block.SurfPaperNmsBlockPackets
import dev.slne.surf.api.paper.nms.bridges.packets.entity.SurfPaperNmsSpawnPackets
import dev.slne.surf.api.paper.nms.bridges.packets.player.SurfPaperNmsPlayerChatPackets
import dev.slne.surf.api.paper.nms.bridges.packets.player.SurfPaperNmsPlayerPackets
import dev.slne.surf.api.paper.nms.bridges.packets.player.SurfPaperNmsPlayerToastPackets
import dev.slne.surf.api.paper.nms.common.*
import dev.slne.surf.api.paper.packet.listener.listener.PacketListener
import dev.slne.surf.api.paper.region.TickThreadGuard
import dev.slne.surf.api.paper.server.nms.v26_2.bridges.*
import dev.slne.surf.api.paper.server.nms.v26_2.bridges.packets.V26_2SurfPaperNmsPacketBridgesImpl
import dev.slne.surf.api.paper.server.nms.v26_2.bridges.packets.block.V26_2SurfPaperNmsBlockPacketsImpl
import dev.slne.surf.api.paper.server.nms.v26_2.bridges.packets.entity.V26_2SurfPaperNmsSpawnPacketsImpl
import dev.slne.surf.api.paper.server.nms.v26_2.bridges.packets.player.V26_2SurfPaperNmsPlayerChatPacketsImpl
import dev.slne.surf.api.paper.server.nms.v26_2.bridges.packets.player.V26_2SurfPaperNmsPlayerPacketsImpl
import dev.slne.surf.api.paper.server.nms.v26_2.bridges.packets.player.V26_2SurfPaperNmsPlayerToastPacketsImpl
import dev.slne.surf.api.paper.server.nms.v26_2.glow.V26_2GlowingLifecycleHandler
import dev.slne.surf.api.paper.server.nms.v26_2.glow.V26_2SurfGlowingApiImpl
import dev.slne.surf.api.paper.server.nms.v26_2.packet.listener.V26_2ChannelInjector
import dev.slne.surf.api.paper.server.nms.v26_2.packet.listener.V26_2CommandSendPacketBlockerListenerImpl
import dev.slne.surf.api.paper.server.nms.v26_2.packet.listener.V26_2GlowingPacketListener
import dev.slne.surf.api.paper.server.nms.v26_2.packet.lore.V26_2PacketLoreListener
import dev.slne.surf.api.paper.server.nms.v26_2.packet.lore.V26_2PacketLoreRegistry
import dev.slne.surf.api.paper.server.nms.v26_2.reflection.V26_2Reflection
import dev.slne.surf.api.paper.server.nms.v26_2.region.V26_2TickThreadGuard
import dev.slne.surf.api.shared.internal.nms.NmsProviderMarker
import dev.slne.surf.api.shared.internal.nms.NmsVersion
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

@Suppress("ClassName")
@OptIn(NmsUseWithCaution::class)
@NmsProviderMarker(NmsVersion.V26_2)
class V26_2NmsProvider(override val plugin: JavaPlugin) : NmsProvider {
    override val version: NmsVersion = NmsVersion.V26_2

    override fun createNmsBridge() =
        V26_2SurfPaperNmsBridgeImpl()

    override fun createCommonBridge(): SurfPaperNmsCommonBridge =
        V26_2SurfPaperNmsCommonBridgeImpl()

    override fun createEntityBridge(): SurfPaperNmsEntityBridge =
        V26_2SurfPaperNmsEntityBridgeImpl()

    override fun createPlayerBridge(): SurfPaperNmsPlayerBridge =
        V26_2SurfPaperNmsPlayerBridgeImpl()

    override fun createItemBridge(): SurfPaperNmsItemBridge =
        V26_2SurfPaperNmsItemBridgeImpl()

    override fun createNbtBridge(): SurfPaperNmsNbtBridge =
        V26_2SurfPaperNmsNbtBridgeImpl()

    override fun createGlowingBridge(): SurfPaperNmsGlowingBridge =
        V26_2SurfPaperNmsGlowingBridgeImpl

    override fun createStatsBridge(): SurfPaperNmsStatsBridge =
        V26_2SurfPaperNmsStatsBridgeImpl()

    override fun createLootTableBridge(): SurfPaperNmsLootTableBridge =
        V26_2SurfPaperNmsLootTableBridgeImpl()

    override fun createCommandArgumentTypesBridge(): SurfPaperNmsCommandArgumentTypesBridge =
        V26_2SurfPaperNmsCommandArgumentTypesBridgeImpl()

    override fun createPacketBridges(): SurfPaperNmsPacketBridges =
        V26_2SurfPaperNmsPacketBridgesImpl()

    override fun createBlockPackets(): SurfPaperNmsBlockPackets =
        V26_2SurfPaperNmsBlockPacketsImpl()

    override fun createSpawnPackets(): SurfPaperNmsSpawnPackets =
        V26_2SurfPaperNmsSpawnPacketsImpl()

    override fun createPlayerPackets(): SurfPaperNmsPlayerPackets =
        V26_2SurfPaperNmsPlayerPacketsImpl()

    override fun createPlayerChatPackets(): SurfPaperNmsPlayerChatPackets =
        V26_2SurfPaperNmsPlayerChatPacketsImpl()

    override fun createPlayerToastPackets(): SurfPaperNmsPlayerToastPackets =
        V26_2SurfPaperNmsPlayerToastPacketsImpl()

    override fun createTickThreadGuard(): TickThreadGuard =
        V26_2TickThreadGuard()

    override fun getLibLoaderBridge(): LibLoaderBridge =
        V26_2LibLoaderBridgeImpl

    override fun getPacketBridgeHandler(): NmsPacketBridgeHandler =
        V26_2NmsPacketBridgeHandler

    override fun createPacketLoreRegistry(): PacketLoreRegistry =
        V26_2PacketLoreRegistry()

    override fun createGlowingLifecycleHandler(): GlowingLifecycleHandler =
        V26_2GlowingLifecycleHandler()

    override fun createGlowingApi(): SurfGlowingApi =
        V26_2SurfGlowingApiImpl

    override fun createChannelInjector(): AbstractChannelInjector<*> =
        V26_2ChannelInjector

    override fun createPacketListenerApi(): InternalPacketListenerApiBridge =
        V26_2PacketListenerApiImpl()

    override fun createCommandSendPacketBlockerListener(blockedPlayers: Set<UUID>): CommandSendPacketBlockerListener {
        return V26_2CommandSendPacketBlockerListenerImpl(
            blockedPlayers
        )
    }

    override fun createPacketListeners(): List<PacketListener> = listOf(
        V26_2PacketLoreListener,
        V26_2GlowingPacketListener,
    )

    override fun initialize() {
        V26_2Reflection.initialize()
        V26_2SurfPaperNmsItemBridgeImpl.CreativeOrderComparator.init()
    }

    override fun shutdown() {
        // No cleanup needed
    }
}
