package dev.slne.surf.api.core.service

import dev.slne.surf.api.spring.SurfApiBean
import java.util.*

/**
 * Looks up Minecraft player names and UUIDs by asking Mojang, with Minecraft Services and Minetools
 * as fallbacks.
 *
 * A bean of surf-api's root context; inject it. Results are cached: found players for 15 minutes,
 * unknown players for 5 minutes, and failures or rate limits for 1 minute, so a failed lookup is
 * retried soon without hammering the APIs.
 */
@SurfApiBean
interface PlayerLookupService {

    /**
     * Returns the current name of the player with [uuid], or `null` if the player does not exist or
     * no API answered.
     */
    suspend fun getUsername(uuid: UUID): String?

    /**
     * Returns the UUID of the player named [username], or `null` if the player does not exist or no
     * API answered.
     */
    suspend fun getUuid(username: String): UUID?

    /**
     * Returns the hit rates and sizes of both lookup caches.
     */
    fun getCacheStats(): CacheStats

    /**
     * Hit rates and sizes of the name-to-UUID and UUID-to-name caches.
     */
    data class CacheStats(
        val nameToUuidHitRate: Double,
        val nameToUuidSize: Long,
        val uuidToNameHitRate: Double,
        val uuidToNameSize: Long,
    )
}