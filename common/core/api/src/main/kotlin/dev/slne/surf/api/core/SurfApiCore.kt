package dev.slne.surf.api.core

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.SurfApiBean
import dev.slne.surf.api.spring.surfBean
import java.util.*

/**
 * The main API class for the SurfCoreApi.
 */
@SurfApiBean
interface SurfApiCore {

    /**
     * Sends a player to a specified server.
     *
     * @param playerUuid the UUID of the player to send
     * @param server     the name of the server to send the player to
     */
    fun sendPlayerToServer(playerUuid: UUID, server: String)

    /**
     * Returns the platform-specific player object for the specified player.
     */
    fun getPlayer(playerUuid: UUID): Any?

    @InternalSurfApi
    companion object {

        /**
         * The instance of the SurfCoreApi.
         */
        @InternalSurfApi
        val instance by surfBean<SurfApiCore>()
    }
}