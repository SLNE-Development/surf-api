package dev.slne.surf.api.paper.nms.common

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.paper.glow.SurfGlowingApi
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.*
import dev.slne.surf.api.paper.nms.bridges.packets.SurfPaperNmsPacketBridges
import dev.slne.surf.api.paper.nms.bridges.packets.block.SurfPaperNmsBlockPackets
import dev.slne.surf.api.paper.nms.bridges.packets.entity.SurfPaperNmsSpawnPackets
import dev.slne.surf.api.paper.nms.bridges.packets.player.SurfPaperNmsPlayerChatPackets
import dev.slne.surf.api.paper.nms.bridges.packets.player.SurfPaperNmsPlayerPackets
import dev.slne.surf.api.paper.nms.bridges.packets.player.SurfPaperNmsPlayerToastPackets
import dev.slne.surf.api.paper.packet.listener.listener.PacketListener
import dev.slne.surf.api.paper.region.TickThreadGuard
import dev.slne.surf.api.shared.internal.nms.NmsProviderConfig
import dev.slne.surf.api.shared.internal.nms.NmsProviderMeta
import dev.slne.surf.api.shared.internal.nms.NmsVersion
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.net.URI
import java.util.jar.JarFile

/**
 * Main interface for version-specific NMS operations.
 *
 * Each supported Minecraft version provides an implementation of this interface
 * that creates the appropriate NMS bridge implementations and packet listeners.
 */
@NmsUseWithCaution
interface NmsProvider {
    /**
     * The NMS version this provider supports.
     */
    val version: NmsVersion
    val plugin: JavaPlugin

    // ==================== Bridge Factories ==================== //

    fun createNmsBridge(): InternalNmsBridge
    fun createCommonBridge(): SurfPaperNmsCommonBridge
    fun createEntityBridge(): SurfPaperNmsEntityBridge
    fun createPlayerBridge(): SurfPaperNmsPlayerBridge
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
    fun getLibLoaderBridge(): LibLoaderBridge

    // ==================== Packet Bridge Handler ==================== //

    /**
     * Creates the packet bridge handler for wrapping/unwrapping NMS packets.
     */
    fun getPacketBridgeHandler(): NmsPacketBridgeHandler

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

    fun createChannelInjector(): AbstractChannelInjector<*>
    fun createPacketListenerApi(): InternalPacketListenerApiBridge

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
        private val log = logger()

        /**
         * Loads the [NmsProvider] for the currently running Minecraft version.
         *
         * @throws IllegalStateException if no provider is found for the current version
         */
        val current: NmsProvider by lazy {
            val version = NmsVersionResolver.current

            try {
                val metas = mutableListOf<NmsProviderMeta>()

                val resources = javaClass.classLoader.getResources(NmsProviderConfig.NMS_PROVIDERS_DIRECTORY)
                while (resources.hasMoreElements()) {
                    val url = resources.nextElement()

                    if (url.protocol == "jar") {
                        val jarPath = url.path.substringBefore("!")
                        JarFile(File(URI(jarPath))).use { jarFile ->
                            jarFile.entries().asSequence()
                                .filter {
                                    it.name.startsWith(NmsProviderConfig.NMS_PROVIDERS_DIRECTORY) && it.name.endsWith(
                                        ".json"
                                    )
                                }
                                .forEach { entry ->
                                    try {
                                        val raw = jarFile.getInputStream(entry).bufferedReader()
                                            .use { it.readText() }
                                        val decoded =
                                            NmsProviderConfig.json.decodeFromString<List<NmsProviderMeta>>(raw)
                                        metas += decoded
                                    } catch (e: Exception) {
                                        log.atSevere()
                                            .withCause(e)
                                            .log("Failed to parse ${entry.name}")
                                    }
                                }
                        }
                    } else {
                        val dir = File(url.toURI())
                        dir.listFiles { file -> file.extension == "json" }?.forEach { file ->
                            try {
                                val raw = file.readText()
                                val decoded =
                                    NmsProviderConfig.json.decodeFromString<List<NmsProviderMeta>>(raw)
                                metas += decoded
                            } catch (e: Exception) {
                                log.atSevere()
                                    .withCause(e)
                                    .log("Failed to parse ${file.name}")
                            }
                        }
                    }
                }

                log.atInfo().log("Discovered %s NmsProviders: %s", metas.size, metas.joinToString(", "))

                val selectedMeta = metas.find { it.version == version }?.also { meta ->
                    log.atInfo().log("Found matching NmsProvider: %s", meta.version.versionPrefix)
                } ?: run {
                    log.atWarning()
                        .log("No exact match for NmsProvider version %s, using fallback", version.versionPrefix)
                    val fallback = metas.maxByOrNull { it.version.versionPrefix }
                        ?: error("No NmsProvider implementations found")
                    log.atWarning().log("Selected fallback NmsProvider: %s", fallback.version.versionPrefix)
                    fallback
                }

                val clazz = try {
                    Class.forName(selectedMeta.implementation)
                } catch (e: Throwable) {
                    throw IllegalStateException("Failed to load NmsProvider implementation", e)
                }

                val constructor = try {
                    clazz.getConstructor(JavaPlugin::class.java)
                } catch (e: Throwable) {
                    throw IllegalStateException(
                        "Failed to load NmsProvider implementation. Implementation must have a constructor with a single argument of type JavaPlugin",
                        e
                    )
                }

                constructor.newInstance(JavaPlugin.getProvidingPlugin(NmsProvider::class.java)) as NmsProvider
            } catch (e: Exception) {
                log.atSevere().withCause(e).log("Failed to load NmsProvider")
                throw IllegalStateException("Failed to load NmsProvider", e)
            }
        }
    }
}
