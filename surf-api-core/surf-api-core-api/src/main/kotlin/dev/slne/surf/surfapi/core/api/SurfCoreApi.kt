package dev.slne.surf.surfapi.core.api

import dev.slne.surf.surfapi.core.api.util.requiredService
import java.util.*

/**
 * The main API class for the SurfCoreApi.
 */
interface SurfCoreApi {

    /**
     * Sends a player to a specified server.
     *
     * @param playerUuid the UUID of the player to send
     * @param server     the name of the server to send the player to
     */
    fun sendPlayerToServer(playerUuid: UUID, server: String)

    fun getPlayer(playerUuid: UUID): Any?

    companion object {
        @JvmStatic
        val instance = requiredService<SurfCoreApi>()
    }
}

val surfCoreApi get() = SurfCoreApi.instance