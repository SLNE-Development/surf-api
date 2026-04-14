package dev.slne.surf.api.paper.nms.common

import dev.slne.surf.api.paper.glow.SurfGlowingApi
import dev.slne.surf.api.paper.nms.SurfPaperNmsBridge
import dev.slne.surf.api.paper.nms.bridges.*
import dev.slne.surf.api.paper.nms.bridges.packets.SurfPaperNmsPacketBridges
import dev.slne.surf.api.paper.nms.bridges.packets.block.SurfPaperNmsBlockPackets
import dev.slne.surf.api.paper.nms.bridges.packets.entity.SurfPaperNmsSpawnPackets
import dev.slne.surf.api.paper.nms.bridges.packets.player.SurfPaperNmsPlayerChatPackets
import dev.slne.surf.api.paper.nms.bridges.packets.player.SurfPaperNmsPlayerPackets
import dev.slne.surf.api.paper.nms.bridges.packets.player.SurfPaperNmsPlayerToastPackets
import dev.slne.surf.api.paper.packet.listener.listener.PacketListener
import dev.slne.surf.api.paper.region.TickThreadGuard

/**
 * Main interface for version-specific NMS operations.
 *
 * Each supported Minecraft version provides an implementation of this interface
 * that creates the appropriate NMS bridge implementations and packet listeners.
 *
 * Implementations are discovered at runtime using [java.util.ServiceLoader].
 */
interface NmsProvider {
    /**
     * The NMS version this provider supports.
     */
    val version: NmsVersion

    // ==================== Bridge Factories ==================== //

    fun createNmsBridge(): SurfPaperNmsBridge
    fun createCommonBridge(): SurfPaperNmsCommonBridge
    fun createEntityBridge(): SurfPaperNmsEntityBridge
    fun createItemBridge(): SurfPaperNmsItemBridge
    fun createNbtBridge(): SurfPaperNmsNbtBridge
    fun createGlowingBridge(): SurfPaperNmsGlowingBridge
    fun createStatsBridge(): SurfPaperNmsStatsBridge
    fun createLootTableBridge(): SurfPaperNmsLootTableBridge
    fun createCommandArgumentTypesBridge(): SurfPaperNmsCommandArgumentTypesBridge
    fun createPacketBridges(): SurfPaperNmsPacketBridges
    fun createBlockPackets(): SurfPaperNmsBlockPackets
    fun createSpawnPackets(): SurfPaperNmsSpawnPackets
    fun createPlayerPackets(): SurfPaperNmsPlayerPackets
    fun createPlayerChatPackets(): SurfPaperNmsPlayerChatPackets
    fun createPlayerToastPackets(): SurfPaperNmsPlayerToastPackets
    fun createTickThreadGuard(): TickThreadGuard

    // ==================== Packet Bridge Handler ==================== //

    /**
     * Creates the packet bridge handler for wrapping/unwrapping NMS packets.
     */
    fun createPacketBridgeHandler(): NmsPacketBridgeHandler

    /**
     * Creates the packet lore registry for managing version-specific lore handlers.
     */
    fun createPacketLoreRegistry(): PacketLoreRegistry

    /**
     * Creates the glowing lifecycle handler for event-driven glowing management.
     */
    fun createGlowingLifecycleHandler(): GlowingLifecycleHandler

    // ==================== Glowing API ==================== //

    /**
     * Creates the version-specific glowing API implementation.
     */
    fun createGlowingApi(): SurfGlowingApi

    // ==================== Packet Listeners ==================== //

    /**
     * Creates version-specific packet listeners (e.g. lore handler, glowing handler).
     *
     * @return list of packet listeners to be registered with the packet listener API
     */
    fun createPacketListeners(): List<PacketListener>

    /**
     * Initializes version-specific resources. Called during plugin enable.
     */
    fun initialize()

    /**
     * Cleans up version-specific resources. Called during plugin disable.
     */
    fun shutdown()

    companion object {
        private val log = dev.slne.surf.api.core.util.logger()

        /**
         * Loads the [NmsProvider] for the currently running Minecraft version.
         *
         * @throws IllegalStateException if no provider is found for the current version
         */
        val current: NmsProvider by lazy {
            val version = NmsVersion.current
            val serviceLoader = java.util.ServiceLoader.load(
                NmsProvider::class.java,
                NmsProvider::class.java.classLoader
            )

            val providers = buildList {
                val iterator = serviceLoader.iterator()
                while (iterator.hasNext()) {
                    try {
                        add(iterator.next())
                    } catch (e: java.util.ServiceConfigurationError) {
                        log.atWarning().withCause(e).log("Skipping NmsProvider that failed to load")
                    }
                }
            }

            log.atInfo().log("Looking for NmsProvider with version: %s", version)
            log.atInfo().log(
                "Available NmsProviders: %s",
                providers.joinToString(", ") { "${it.version.name} (${it.version.versionPrefix})" }
            )

            val matched = providers.firstOrNull { it.version == version }
            if (matched != null) {
                log.atInfo().log("Found matching NmsProvider: %s", matched.version.name)
                matched
            } else {
                log.atWarning()
                    .log("No exact match for NmsProvider version %s, using fallback", version)
                val fallback = providers.maxByOrNull { it.version.versionPrefix }
                    ?: error(
                        "No NmsProvider implementations found for version $version. " +
                                "Ensure the correct NMS module is included in the classpath."
                    )
                log.atWarning().log("Selected fallback NmsProvider: %s", fallback.version.name)
                fallback
            }
        }
    }
}
