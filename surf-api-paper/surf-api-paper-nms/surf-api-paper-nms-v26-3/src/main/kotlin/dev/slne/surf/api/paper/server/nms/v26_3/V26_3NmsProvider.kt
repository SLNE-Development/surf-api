package dev.slne.surf.api.paper.server.nms.v26_3

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
import dev.slne.surf.api.paper.server.nms.v26_3.bridges.*
import dev.slne.surf.api.paper.server.nms.v26_3.bridges.packets.V26_3SurfPaperNmsPacketBridgesImpl
import dev.slne.surf.api.paper.server.nms.v26_3.bridges.packets.block.V26_3SurfPaperNmsBlockPacketsImpl
import dev.slne.surf.api.paper.server.nms.v26_3.bridges.packets.entity.V26_3SurfPaperNmsSpawnPacketsImpl
import dev.slne.surf.api.paper.server.nms.v26_3.bridges.packets.player.V26_3SurfPaperNmsPlayerChatPacketsImpl
import dev.slne.surf.api.paper.server.nms.v26_3.bridges.packets.player.V26_3SurfPaperNmsPlayerPacketsImpl
import dev.slne.surf.api.paper.server.nms.v26_3.bridges.packets.player.V26_3SurfPaperNmsPlayerToastPacketsImpl
import dev.slne.surf.api.paper.server.nms.v26_3.glow.V26_3GlowingLifecycleHandler
import dev.slne.surf.api.paper.server.nms.v26_3.glow.V26_3SurfGlowingApiImpl
import dev.slne.surf.api.paper.server.nms.v26_3.packet.listener.V26_3ChannelInjector
import dev.slne.surf.api.paper.server.nms.v26_3.packet.listener.V26_3CommandSendPacketBlockerListenerImpl
import dev.slne.surf.api.paper.server.nms.v26_3.packet.listener.V26_3GlowingPacketListener
import dev.slne.surf.api.paper.server.nms.v26_3.packet.lore.V26_3PacketLoreListener
import dev.slne.surf.api.paper.server.nms.v26_3.packet.lore.V26_3PacketLoreRegistry
import dev.slne.surf.api.paper.server.nms.v26_3.reflection.V26_3Reflection
import dev.slne.surf.api.paper.server.nms.v26_3.region.V26_3TickThreadGuard
import dev.slne.surf.api.shared.internal.nms.NmsProviderMarker
import dev.slne.surf.api.shared.internal.nms.NmsVersion
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

@Suppress("ClassName")
@OptIn(NmsUseWithCaution::class)
@NmsProviderMarker(NmsVersion.V26_3)
class V26_3NmsProvider(override val plugin: JavaPlugin) : NmsProvider {
    override val version: NmsVersion = NmsVersion.V26_3

    override fun createNmsBridge() = V26_3SurfPaperNmsBridgeImpl()
    override fun createCommonBridge(): SurfPaperNmsCommonBridge =
        V26_3SurfPaperNmsCommonBridgeImpl()

    override fun createEntityBridge(): SurfPaperNmsEntityBridge = V26_3SurfPaperNmsEntityBridgeImpl()
    override fun createPortalBridge(): SurfPaperNmsPortalBridge = V26_3SurfPaperNmsPortalBridgeImpl()
    override fun createPlayerBridge(): SurfPaperNmsPlayerBridge = V26_3SurfPaperNmsPlayerBridgeImpl()
    override fun createItemBridge(): SurfPaperNmsItemBridge = V26_3SurfPaperNmsItemBridgeImpl()
    override fun createNbtBridge(): SurfPaperNmsNbtBridge = V26_3SurfPaperNmsNbtBridgeImpl()
    override fun createGlowingBridge(): SurfPaperNmsGlowingBridge = V26_3SurfPaperNmsGlowingBridgeImpl
    override fun createStatsBridge(): SurfPaperNmsStatsBridge = V26_3SurfPaperNmsStatsBridgeImpl()
    override fun createLootTableBridge(): SurfPaperNmsLootTableBridge = V26_3SurfPaperNmsLootTableBridgeImpl()
    override fun createCommandArgumentTypesBridge(): SurfPaperNmsCommandArgumentTypesBridge =
        V26_3SurfPaperNmsCommandArgumentTypesBridgeImpl()

    override fun createPacketBridges(): SurfPaperNmsPacketBridges = V26_3SurfPaperNmsPacketBridgesImpl()

    override fun createBlockPackets(): SurfPaperNmsBlockPackets = V26_3SurfPaperNmsBlockPacketsImpl()
    override fun createSpawnPackets(): SurfPaperNmsSpawnPackets = V26_3SurfPaperNmsSpawnPacketsImpl()
    override fun createPlayerPackets(): SurfPaperNmsPlayerPackets = V26_3SurfPaperNmsPlayerPacketsImpl()
    override fun createPlayerChatPackets(): SurfPaperNmsPlayerChatPackets = V26_3SurfPaperNmsPlayerChatPacketsImpl()
    override fun createPlayerToastPackets(): SurfPaperNmsPlayerToastPackets = V26_3SurfPaperNmsPlayerToastPacketsImpl()

    override fun createTickThreadGuard(): TickThreadGuard = V26_3TickThreadGuard()
    override fun getLibLoaderBridge(): LibLoaderBridge = V26_3LibLoaderBridgeImpl
    override fun getPacketBridgeHandler(): NmsPacketBridgeHandler = V26_3NmsPacketBridgeHandler
    override fun createPacketLoreRegistry(): PacketLoreRegistry = V26_3PacketLoreRegistry()
    override fun createGlowingLifecycleHandler(): GlowingLifecycleHandler = V26_3GlowingLifecycleHandler()
    override fun createGlowingApi(): SurfGlowingApi = V26_3SurfGlowingApiImpl
    override fun createChannelInjector(): AbstractChannelInjector<*> = V26_3ChannelInjector
    override fun createPacketListenerApi(): InternalPacketListenerApiBridge = V26_3PacketListenerApiImpl()

    override fun createCommandSendPacketBlockerListener(blockedPlayers: Set<UUID>): CommandSendPacketBlockerListener {
        return V26_3CommandSendPacketBlockerListenerImpl(blockedPlayers)
    }

    override fun createPacketListeners(): List<PacketListener> = listOf(
        V26_3PacketLoreListener,
        V26_3GlowingPacketListener,
    )

    override fun initialize() {
        V26_3Reflection.initialize()
        V26_3SurfPaperNmsItemBridgeImpl.CreativeOrderComparator.init()
    }

    override fun shutdown() {
        // No cleanup needed
    }
}
