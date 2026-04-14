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
import dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.*
import dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.packets.V1_21_11SurfPaperNmsPacketBridgesImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.packets.block.V1_21_11SurfPaperNmsBlockPacketsImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.packets.entity.V1_21_11SurfPaperNmsSpawnPacketsImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.packets.player.V1_21_11SurfPaperNmsPlayerChatPacketsImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.packets.player.V1_21_11SurfPaperNmsPlayerPacketsImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.packets.player.V1_21_11SurfPaperNmsPlayerToastPacketsImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.glow.V1_21_11GlowingLifecycleHandler
import dev.slne.surf.api.paper.server.nms.v1_21_11.glow.V1_21_11SurfGlowingApiImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.packet.listener.V1_21_11GlowingPacketListener
import dev.slne.surf.api.paper.server.nms.v1_21_11.packet.lore.V1_21_11PacketLoreListener
import dev.slne.surf.api.paper.server.nms.v1_21_11.packet.lore.V1_21_11PacketLoreRegistry
import dev.slne.surf.api.paper.server.nms.v1_21_11.reflection.V1_21_11Reflection
import dev.slne.surf.api.paper.server.nms.v1_21_11.region.V1_21_11TickThreadGuard
import org.bukkit.plugin.java.JavaPlugin

@AutoService(NmsProvider::class)
class V1_21_11NmsProvider : NmsProvider {
    override val version: NmsVersion = NmsVersion.V1_21_11

    private var glowingApi: V1_21_11SurfGlowingApiImpl? = null

    override fun createNmsBridge(): SurfPaperNmsBridge = V1_21_11SurfPaperNmsBridgeImpl()
    override fun createCommonBridge(): SurfPaperNmsCommonBridge = V1_21_11SurfPaperNmsCommonBridgeImpl()
    override fun createEntityBridge(): SurfPaperNmsEntityBridge = V1_21_11SurfPaperNmsEntityBridgeImpl()
    override fun createItemBridge(): SurfPaperNmsItemBridge = V1_21_11SurfPaperNmsItemBridgeImpl()
    override fun createNbtBridge(): SurfPaperNmsNbtBridge = V1_21_11SurfPaperNmsNbtBridgeImpl()
    override fun createGlowingBridge(): SurfPaperNmsGlowingBridge = V1_21_11SurfPaperNmsGlowingBridgeImpl()
    override fun createStatsBridge(): SurfPaperNmsStatsBridge = V1_21_11SurfPaperNmsStatsBridgeImpl()
    override fun createLootTableBridge(): SurfPaperNmsLootTableBridge = V1_21_11SurfPaperNmsLootTableBridgeImpl()
    override fun createCommandArgumentTypesBridge(): SurfPaperNmsCommandArgumentTypesBridge = V1_21_11SurfPaperNmsCommandArgumentTypesBridgeImpl()
    override fun createPacketBridges(): SurfPaperNmsPacketBridges = V1_21_11SurfPaperNmsPacketBridgesImpl()
    override fun createBlockPackets(): SurfPaperNmsBlockPackets = V1_21_11SurfPaperNmsBlockPacketsImpl()
    override fun createSpawnPackets(): SurfPaperNmsSpawnPackets = V1_21_11SurfPaperNmsSpawnPacketsImpl()
    override fun createPlayerPackets(): SurfPaperNmsPlayerPackets = V1_21_11SurfPaperNmsPlayerPacketsImpl()
    override fun createPlayerChatPackets(): SurfPaperNmsPlayerChatPackets = V1_21_11SurfPaperNmsPlayerChatPacketsImpl()
    override fun createPlayerToastPackets(): SurfPaperNmsPlayerToastPackets = V1_21_11SurfPaperNmsPlayerToastPacketsImpl()
    override fun createTickThreadGuard(): TickThreadGuard = V1_21_11TickThreadGuard()
    override fun createPacketBridgeHandler(): NmsPacketBridgeHandler = V1_21_11NmsPacketBridgeHandler()
    override fun createPacketLoreRegistry(): PacketLoreRegistry = V1_21_11PacketLoreRegistry()
    override fun createGlowingLifecycleHandler(): GlowingLifecycleHandler = V1_21_11GlowingLifecycleHandler()

    override fun createGlowingApi(): SurfGlowingApi {
        val plugin = JavaPlugin.getProvidingPlugin(V1_21_11NmsProvider::class.java) as JavaPlugin
        val api = V1_21_11SurfGlowingApiImpl(plugin)
        glowingApi = api
        return api
    }

    override fun createPacketListeners(): List<PacketListener> = listOf(
        V1_21_11PacketLoreListener,
        V1_21_11GlowingPacketListener,
    )

    override fun initialize() {
        V1_21_11Reflection.initialize()
    }

    override fun shutdown() {
        // No cleanup needed
    }
}
