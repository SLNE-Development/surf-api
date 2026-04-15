package dev.slne.surf.api.paper.server.nms.v26_1

import com.google.auto.service.AutoService
import dev.slne.surf.api.paper.glow.SurfGlowingApi
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.SurfPaperNmsBridge
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
import dev.slne.surf.api.paper.server.nms.v26_1.bridges.*
import dev.slne.surf.api.paper.server.nms.v26_1.bridges.packets.V26_1SurfPaperNmsPacketBridgesImpl
import dev.slne.surf.api.paper.server.nms.v26_1.bridges.packets.block.V26_1SurfPaperNmsBlockPacketsImpl
import dev.slne.surf.api.paper.server.nms.v26_1.bridges.packets.entity.V26_1SurfPaperNmsSpawnPacketsImpl
import dev.slne.surf.api.paper.server.nms.v26_1.bridges.packets.player.V26_1SurfPaperNmsPlayerChatPacketsImpl
import dev.slne.surf.api.paper.server.nms.v26_1.bridges.packets.player.V26_1SurfPaperNmsPlayerPacketsImpl
import dev.slne.surf.api.paper.server.nms.v26_1.bridges.packets.player.V26_1SurfPaperNmsPlayerToastPacketsImpl
import dev.slne.surf.api.paper.server.nms.v26_1.glow.V26_1GlowingLifecycleHandler
import dev.slne.surf.api.paper.server.nms.v26_1.glow.V26_1SurfGlowingApiImpl
import dev.slne.surf.api.paper.server.nms.v26_1.packet.listener.V26_1GlowingPacketListener
import dev.slne.surf.api.paper.server.nms.v26_1.packet.lore.V26_1PacketLoreListener
import dev.slne.surf.api.paper.server.nms.v26_1.packet.lore.V26_1PacketLoreRegistry
import dev.slne.surf.api.paper.server.nms.v26_1.reflection.V26_1Reflection
import dev.slne.surf.api.paper.server.nms.v26_1.region.V26_1TickThreadGuard

@Suppress("ClassName")
@OptIn(NmsUseWithCaution::class)
@AutoService(NmsProvider::class)
class V26_1NmsProvider : NmsProvider {
    override val version: NmsVersion = NmsVersion.V26_1

    override fun createNmsBridge(): SurfPaperNmsBridge = V26_1SurfPaperNmsBridgeImpl()
    override fun createCommonBridge(): SurfPaperNmsCommonBridge =
        V26_1SurfPaperNmsCommonBridgeImpl()

    override fun createEntityBridge(): SurfPaperNmsEntityBridge =
        V26_1SurfPaperNmsEntityBridgeImpl()

    override fun createItemBridge(): SurfPaperNmsItemBridge = V26_1SurfPaperNmsItemBridgeImpl()
    override fun createNbtBridge(): SurfPaperNmsNbtBridge = V26_1SurfPaperNmsNbtBridgeImpl()
    override fun createGlowingBridge(): SurfPaperNmsGlowingBridge = V26_1SurfPaperNmsGlowingBridgeImpl

    override fun createStatsBridge(): SurfPaperNmsStatsBridge =
        V26_1SurfPaperNmsStatsBridgeImpl()

    override fun createLootTableBridge(): SurfPaperNmsLootTableBridge =
        V26_1SurfPaperNmsLootTableBridgeImpl()

    override fun createCommandArgumentTypesBridge(): SurfPaperNmsCommandArgumentTypesBridge =
        V26_1SurfPaperNmsCommandArgumentTypesBridgeImpl()

    override fun createPacketBridges(): SurfPaperNmsPacketBridges =
        V26_1SurfPaperNmsPacketBridgesImpl()

    override fun createBlockPackets(): SurfPaperNmsBlockPackets =
        V26_1SurfPaperNmsBlockPacketsImpl()

    override fun createSpawnPackets(): SurfPaperNmsSpawnPackets =
        V26_1SurfPaperNmsSpawnPacketsImpl()

    override fun createPlayerPackets(): SurfPaperNmsPlayerPackets =
        V26_1SurfPaperNmsPlayerPacketsImpl()

    override fun createPlayerChatPackets(): SurfPaperNmsPlayerChatPackets =
        V26_1SurfPaperNmsPlayerChatPacketsImpl()

    override fun createPlayerToastPackets(): SurfPaperNmsPlayerToastPackets =
        V26_1SurfPaperNmsPlayerToastPacketsImpl()

    override fun createTickThreadGuard(): TickThreadGuard = V26_1TickThreadGuard()
    override fun getPacketBridgeHandler(): NmsPacketBridgeHandler = V26_1NmsPacketBridgeHandler

    override fun createPacketLoreRegistry(): PacketLoreRegistry = V26_1PacketLoreRegistry()
    override fun createGlowingLifecycleHandler(): GlowingLifecycleHandler =
        V26_1GlowingLifecycleHandler()

    override fun createGlowingApi(): SurfGlowingApi = V26_1SurfGlowingApiImpl

    override fun createPacketListeners(): List<PacketListener> = listOf(
        V26_1PacketLoreListener,
        V26_1GlowingPacketListener,
    )

    override fun initialize() {
        V26_1Reflection.initialize()
    }

    override fun shutdown() {
        // No cleanup needed
    }
}
