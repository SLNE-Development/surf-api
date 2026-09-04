@file:Suppress("UNCHECKED_CAST")

package dev.slne.surf.api.minestom.player

import net.minestom.server.network.ConnectionManager
import net.minestom.server.network.player.PlayerConnection
import java.util.*

/**
 * @see ConnectionManager.getOnlinePlayers
 */
val ConnectionManager.onlineSurfMinestomPlayers: Collection<SurfMinestomPlayer>
    get() = onlinePlayers as Collection<SurfMinestomPlayer>

/**
 * @see ConnectionManager.getConfigPlayers
 */
val ConnectionManager.configSurfMinestomPlayers: Collection<SurfMinestomPlayer>
    get() = configPlayers as Collection<SurfMinestomPlayer>

/**
 * @see ConnectionManager.getPlayer
 */
fun ConnectionManager.getSurfPlayer(
    connection: PlayerConnection,
): SurfMinestomPlayer? = getPlayer(connection) as? SurfMinestomPlayer

/**
 * @see ConnectionManager.getOnlinePlayerByUsername
 */
fun ConnectionManager.getOnlineSurfPlayerByUsername(
    username: String,
): SurfMinestomPlayer? = getOnlinePlayerByUsername(username) as? SurfMinestomPlayer

/**
 * @see ConnectionManager.getOnlinePlayerByUuid
 */
fun ConnectionManager.getOnlineSurfPlayerByUuid(
    uuid: UUID,
): SurfMinestomPlayer? = getOnlinePlayerByUuid(uuid) as? SurfMinestomPlayer

fun ConnectionManager.getSurfPlayer(uuid: UUID): SurfMinestomPlayer? =
    getOnlineSurfPlayerByUuid(uuid)

/**
 * @see ConnectionManager.findOnlinePlayer
 */
fun ConnectionManager.findOnlineSurfPlayer(
    username: String,
): SurfMinestomPlayer? = findOnlinePlayer(username) as? SurfMinestomPlayer

