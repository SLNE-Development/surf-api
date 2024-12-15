package dev.slne.surf.surfapi.core.api

import dev.slne.surf.surfapi.core.api.config.SurfConfigManager.ConfigFileNamePattern
import dev.slne.surf.surfapi.core.api.config.SurfConfigManagerModern
import dev.slne.surf.surfapi.core.api.config.SurfConfigManagerModern.ModernJsonConfigFileNamePattern
import dev.slne.surf.surfapi.core.api.config.SurfConfigManagerModern.ModernYamlConfigFileNamePattern
import dev.slne.surf.surfapi.core.api.packet.SurfCorePacketApi
import dev.slne.surf.surfapi.core.api.reflection.SurfReflection
import dev.slne.surf.surfapi.core.api.util.requiredService
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path
import java.util.*

/**
 * The main API class for the SurfCoreApi.
 */
@ApiStatus.NonExtendable
interface SurfCoreApi {
    /**
     * Retrieves the SurfCorePacketApi instance.
     * It may be used for all packet related operations.
     * @return the SurfCorePacketApi instance
     */
    val packetApi: SurfCorePacketApi?

    /**
     * Sends a player to a specified server.
     *
     * @param playerUuid the UUID of the player to send
     * @param server     the name of the server to send the player to
     */
    fun sendPlayerToServer(playerUuid: UUID?, server: String?)

    fun getPlayer(playerUuid: UUID): Optional<Any>

    @get:ApiStatus.Experimental
    val reflection: SurfReflection

    @Deprecated("")
    fun <C> createConfig(
        configClass: Class<C?>, configFolder: Path,
        configFileName: @ConfigFileNamePattern String
    ): C?

    @Deprecated("")
    fun <C> getConfig(configClass: Class<C?>): C?

    @Deprecated("")
    fun <C> reloadConfig(configClass: Class<C?>): C?

    fun <C> createModernYamlConfig(
        configClass: Class<C?>, configFolder: Path,
        configFileName: @ModernYamlConfigFileNamePattern String
    ): C?

    fun <C> createModernJsonConfig(
        configClass: Class<C?>, configFolder: Path,
        configFileName: @ModernJsonConfigFileNamePattern String
    ): C?

    fun <C> getModernConfig(configClass: Class<C?>): C?

    fun <C> reloadModernConfig(configClass: Class<C?>): C?

    fun <C> getModernConfigManager(configClass: Class<C?>): SurfConfigManagerModern<C?>?

    val dataFolder: Path

    companion object {
        @JvmStatic
        val core = requiredService<SurfCoreApi>()
    }
}
