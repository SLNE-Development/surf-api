package dev.slne.surf.api.paper.server.nms.v1_21_11

import com.google.auto.service.AutoService
import dev.slne.surf.api.paper.glow.SurfGlowingApi
import dev.slne.surf.api.paper.nms.SurfPaperNmsBridge
import dev.slne.surf.api.paper.nms.bridges.*
import dev.slne.surf.api.paper.nms.bridges.packets.SurfPaperNmsPacketBridges
import dev.slne.surf.api.paper.nms.bridges.packets.block.SurfPaperNmsBlockPackets
import dev.slne.surf.api.paper.nms.bridges.packets.entity.SurfPaperNmsSpawnPackets
import dev.slne.surf.api.paper.nms.bridges.packets.player.SurfPaperNmsPlayerChatPackets
import dev.slne.surf.api.paper.nms.bridges.packets.player.SurfPaperNmsPlayerPackets
import dev.slne.surf.api.paper.nms.bridges.packets.player.SurfPaperNmsPlayerToastPackets
import dev.slne.surf.api.paper.nms.common.GlowingLifecycleHandler
import dev.slne.surf.api.paper.nms.common.NmsPacketBridgeHandler
import dev.slne.surf.api.paper.nms.common.NmsProvider
import dev.slne.surf.api.paper.nms.common.NmsVersion
import dev.slne.surf.api.paper.nms.common.PacketLoreRegistry
import dev.slne.surf.api.paper.packet.listener.listener.PacketListener
import dev.slne.surf.api.paper.region.TickThreadGuard

/**
 * NMS provider for Minecraft 1.21.11.
 *
 * TODO: Implement all methods with 1.21.11-specific NMS code.
 * Each method should return an implementation adapted for the 1.21.11 NMS API.
 * Use the V26_1 implementations as a reference and adapt for API differences.
 */
@AutoService(NmsProvider::class)
class V1_21_11NmsProvider : NmsProvider {
    override val version: NmsVersion = NmsVersion.V1_21_11

    override fun createNmsBridge(): SurfPaperNmsBridge =
        TODO("Implement SurfPaperNmsBridge for 1.21.11")

    override fun createCommonBridge(): SurfPaperNmsCommonBridge =
        TODO("Implement SurfPaperNmsCommonBridge for 1.21.11")

    override fun createEntityBridge(): SurfPaperNmsEntityBridge =
        TODO("Implement SurfPaperNmsEntityBridge for 1.21.11")

    override fun createItemBridge(): SurfPaperNmsItemBridge =
        TODO("Implement SurfPaperNmsItemBridge for 1.21.11")

    override fun createNbtBridge(): SurfPaperNmsNbtBridge =
        TODO("Implement SurfPaperNmsNbtBridge for 1.21.11")

    override fun createGlowingBridge(): SurfPaperNmsGlowingBridge =
        TODO("Implement SurfPaperNmsGlowingBridge for 1.21.11")

    override fun createStatsBridge(): SurfPaperNmsStatsBridge =
        TODO("Implement SurfPaperNmsStatsBridge for 1.21.11")

    override fun createLootTableBridge(): SurfPaperNmsLootTableBridge =
        TODO("Implement SurfPaperNmsLootTableBridge for 1.21.11")

    override fun createCommandArgumentTypesBridge(): SurfPaperNmsCommandArgumentTypesBridge =
        TODO("Implement SurfPaperNmsCommandArgumentTypesBridge for 1.21.11")

    override fun createPacketBridges(): SurfPaperNmsPacketBridges =
        TODO("Implement SurfPaperNmsPacketBridges for 1.21.11")

    override fun createBlockPackets(): SurfPaperNmsBlockPackets =
        TODO("Implement SurfPaperNmsBlockPackets for 1.21.11")

    override fun createSpawnPackets(): SurfPaperNmsSpawnPackets =
        TODO("Implement SurfPaperNmsSpawnPackets for 1.21.11")

    override fun createPlayerPackets(): SurfPaperNmsPlayerPackets =
        TODO("Implement SurfPaperNmsPlayerPackets for 1.21.11")

    override fun createPlayerChatPackets(): SurfPaperNmsPlayerChatPackets =
        TODO("Implement SurfPaperNmsPlayerChatPackets for 1.21.11")

    override fun createPlayerToastPackets(): SurfPaperNmsPlayerToastPackets =
        TODO("Implement SurfPaperNmsPlayerToastPackets for 1.21.11")

    override fun createTickThreadGuard(): TickThreadGuard =
        TODO("Implement TickThreadGuard for 1.21.11")

    override fun createPacketBridgeHandler(): NmsPacketBridgeHandler =
        TODO("Implement NmsPacketBridgeHandler for 1.21.11")

    override fun createPacketLoreRegistry(): PacketLoreRegistry =
        TODO("Implement PacketLoreRegistry for 1.21.11")

    override fun createGlowingLifecycleHandler(): GlowingLifecycleHandler =
        TODO("Implement GlowingLifecycleHandler for 1.21.11")

    override fun createGlowingApi(): SurfGlowingApi =
        TODO("Implement SurfGlowingApi for 1.21.11")

    override fun createPacketListeners(): List<PacketListener> =
        TODO("Implement packet listeners for 1.21.11")

    override fun initialize() {
        TODO("Initialize 1.21.11 NMS resources")
    }

    override fun shutdown() {
        // No cleanup needed
    }
}
