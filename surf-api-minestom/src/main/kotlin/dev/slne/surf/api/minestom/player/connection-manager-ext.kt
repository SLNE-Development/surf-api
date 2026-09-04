@file:Suppress("UNCHECKED_CAST")

package dev.slne.surf.api.minestom.player

import net.minestom.server.network.ConnectionManager
import net.minestom.server.network.player.PlayerConnection
import java.util.*

/**
 * @see ConnectionManager.getOnlinePlayers
 */
val ConnectionManager.onlineSurfPlayers: Collection<SurfPlayer>
    get() = onlinePlayers as Collection<SurfPlayer>

/**
 * @see ConnectionManager.getConfigPlayers
 */
val ConnectionManager.configSurfPlayers: Collection<SurfPlayer>
    get() = configPlayers as Collection<SurfPlayer>

/**
 * @see ConnectionManager.getPlayer
 */
fun ConnectionManager.getSurfPlayer(
    connection: PlayerConnection,
): SurfPlayer? = getPlayer(connection) as? SurfPlayer

/**
 * @see ConnectionManager.getOnlinePlayerByUsername
 */
fun ConnectionManager.getOnlineSurfPlayerByUsername(
    username: String,
): SurfPlayer? = getOnlinePlayerByUsername(username) as? SurfPlayer

/**
 * @see ConnectionManager.getOnlinePlayerByUuid
 */
fun ConnectionManager.getOnlineSurfPlayerByUuid(
    uuid: UUID,
): SurfPlayer? = getOnlinePlayerByUuid(uuid) as? SurfPlayer

fun ConnectionManager.getSurfPlayer(uuid: UUID): SurfPlayer? = getOnlineSurfPlayerByUuid(uuid)

/**
 * @see ConnectionManager.findOnlinePlayer
 */
fun ConnectionManager.findOnlineSurfPlayer(
    username: String,
): SurfPlayer? = findOnlinePlayer(username) as? SurfPlayer

