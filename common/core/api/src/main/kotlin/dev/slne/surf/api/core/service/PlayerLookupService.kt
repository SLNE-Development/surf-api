package dev.slne.surf.api.core.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import com.sksamuel.aedile.core.asLoadingCache
import dev.slne.surf.api.core.util.logger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.time.Duration.Companion.minutes

/**
 * Service for looking up Minecraft player information using their usernames or UUIDs.
 *
 * This service fetches player data primarily from Mojang's API with fallback support via Minetools.
 * It employs caching to minimize API calls and enhance performance.
 */
object PlayerLookupService {

    private val log = logger()

    /** HTTP client configured for JSON requests and responses. */
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                isLenient = true
                encodeDefaults = true
                ignoreUnknownKeys = true
            })
        }
    }

    /**
     * Sealed interface representing the result of a player lookup.
     * This allows caching of non-null values including failures.
     */
    sealed interface LookupResult<out T> {
        /** Successfully found player data. */
        data class Found<T>(val value: T) : LookupResult<T>

        /** Player does not exist. */
        data object NotFound : LookupResult<Nothing>

        /** API rate limit or temporary failure. */
        data class RateLimited(val error: String) : LookupResult<Nothing>

        data class Failed(val error: Throwable) : LookupResult<Nothing>
    }

    /**
     * Custom expiry policy that uses different TTLs based on result type:
     * - Found: 15 minutes (successful lookups)
     * - NotFound: 5 minutes (non-existent players, can be rechecked sooner)
     * - RateLimited: 1 minute (temporary errors, retry soon)
     */
    private class LookupExpiry<K : Any, V> : Expiry<K, LookupResult<V>> {
        override fun expireAfterCreate(
            key: K,
            value: LookupResult<V>,
            currentTime: Long
        ): Long = when (value) {
            is LookupResult.Found -> 15.minutes.inWholeNanoseconds
            is LookupResult.NotFound -> 5.minutes.inWholeNanoseconds
            is LookupResult.RateLimited, is LookupResult.Failed -> 1.minutes.inWholeNanoseconds
        }

        override fun expireAfterUpdate(
            key: K,
            value: LookupResult<V>,
            currentTime: Long,
            currentDuration: Long
        ): Long = expireAfterCreate(key, value, currentTime)

        override fun expireAfterRead(
            key: K,
            value: LookupResult<V>,
            currentTime: Long,
            currentDuration: Long
        ): Long = currentDuration
    }

    /**
     * Cache mapping usernames to UUIDs.
     * Uses custom expiry based on result type.
     */
    private val nameToUuid = Caffeine.newBuilder()
        .expireAfter(LookupExpiry<String, UUID>())
        .recordStats()
        .maximumSize(10_000)
        .asLoadingCache<String, LookupResult<UUID>> { name ->
            lookupUuid(name)
        }

    /**
     * Cache mapping UUIDs to usernames.
     * Uses custom expiry based on result type.
     */
    private val uuidToName = Caffeine.newBuilder()
        .expireAfter(LookupExpiry<UUID, String>())
        .recordStats() // Enables cache statistics
        .maximumSize(10_000)
        .asLoadingCache<UUID, LookupResult<String>> { uuid ->
            lookupUsername(uuid)
        }

    /**
     * Retrieves the username corresponding to the provided UUID.
     * @param uuid UUID of the player.
     * @return Username associated with the UUID or null if not found.
     */
    suspend fun getUsername(uuid: UUID): String? = when (val result = uuidToName.get(uuid)) {
        is LookupResult.Found -> result.value
        is LookupResult.NotFound, is LookupResult.RateLimited, is LookupResult.Failed -> null
    }

    /**
     * Retrieves the UUID corresponding to the provided username.
     * @param username Minecraft username.
     * @return UUID associated with the username or null if not found.
     */
    suspend fun getUuid(username: String): UUID? = when (val result = nameToUuid.get(username)) {
        is LookupResult.Found -> result.value
        is LookupResult.NotFound, is LookupResult.RateLimited, is LookupResult.Failed -> null
    }

    fun getCacheStats(): CacheStats {
        val nameStats = nameToUuid.underlying().synchronous().stats()
        val uuidStats = uuidToName.underlying().synchronous().stats()

        return CacheStats(
            nameToUuidHitRate = nameStats.hitRate(),
            nameToUuidSize = nameToUuid.underlying().synchronous().estimatedSize(),
            uuidToNameHitRate = uuidStats.hitRate(),
            uuidToNameSize = uuidToName.underlying().synchronous().estimatedSize()
        )
    }

    data class CacheStats(
        val nameToUuidHitRate: Double,
        val nameToUuidSize: Long,
        val uuidToNameHitRate: Double,
        val uuidToNameSize: Long
    )

    private suspend fun lookupUuid(name: String): LookupResult<UUID> {
        try {
            val (mojangStatus, mojangUuid) = MojangApi.getUuid(name)
            if (mojangUuid != null && mojangStatus.isSuccess()) {
                return LookupResult.Found(mojangUuid)
            }

            if (mojangStatus == HttpStatusCode.NotFound) return LookupResult.NotFound // No need to check further if Mojang says it doesn't exist

            val (minecraftStatus, minecraftUuid) = MinecraftServicesApi.getUuid(name)
            if (minecraftUuid != null && minecraftStatus.isSuccess()) {
                return LookupResult.Found(minecraftUuid)
            }

            val (mineToolsStatus, mineToolUuid) = MinetoolsApi.getUuid(name)
            if (mineToolUuid != null && mineToolsStatus.isSuccess()) {
                return LookupResult.Found(mineToolUuid)
            }

            return handleLookupError(mineToolsStatus)
        } catch (e: Throwable) {
            log.atWarning()
                .withCause(e)
                .log("Failed to lookup UUID for username $name")
            return LookupResult.Failed(e)
        }
    }

    private suspend fun lookupUsername(uuid: UUID): LookupResult<String> {
        try {
            val (mojangStatus, mojangName) = MojangApi.getUsername(uuid)
            if (mojangName != null && mojangStatus.isSuccess()) {
                return LookupResult.Found(mojangName)
            }

            if (mojangStatus == HttpStatusCode.NotFound) return LookupResult.NotFound // No need to check further if Mojang says it doesn't exist

            val (minecraftStatus, mcName) = MinecraftServicesApi.getUsername(uuid)
            if (mcName != null && minecraftStatus.isSuccess()) {
                return LookupResult.Found(mcName)
            }

            val (mineToolsStatus, mtName) = MinetoolsApi.getUsername(uuid)
            if (mtName != null && mineToolsStatus.isSuccess()) {
                return LookupResult.Found(mtName)
            }

            return handleLookupError(mineToolsStatus)
        } catch (e: Throwable) {
            log.atWarning()
                .withCause(e)
                .log("Failed to lookup username for UUID $uuid")
            return LookupResult.Failed(e)
        }
    }

    private fun <T> handleLookupError(status: HttpStatusCode): LookupResult<T> {
        return when (status) {
            HttpStatusCode.NotFound -> LookupResult.NotFound
            HttpStatusCode.TooManyRequests -> LookupResult.RateLimited("Rate limit exceeded")
            HttpStatusCode.ServiceUnavailable -> LookupResult.RateLimited("Service unavailable")
            HttpStatusCode.GatewayTimeout -> LookupResult.RateLimited("Timeout")
            else -> LookupResult.RateLimited("API error: ${status.value}")
        }
    }

    private object MojangApi {
        private const val BASE_URL = "https://api.mojang.com"

        suspend fun getUsername(uuid: UUID): Pair<HttpStatusCode, String?> {
            val response = client.get("$BASE_URL/user/profile/${UUIDSerializer.fromUUID(uuid)}")
            val status = response.status
            val name = runCatching { response.body<MojangResponse>().name }.getOrNull()
            return status to name
        }

        suspend fun getUuid(username: String): Pair<HttpStatusCode, UUID?> {
            val response = client.get("$BASE_URL/users/profiles/minecraft/$username")
            val status = response.status
            val uuid = runCatching { response.body<MojangResponse>().id }.getOrNull()
            return status to uuid
        }
    }

    private object MinecraftServicesApi {
        private const val BASE_URL = "https://api.minecraftservices.com"

        suspend fun getUsername(uuid: UUID): Pair<HttpStatusCode, String?> {
            val response =
                client.get("$BASE_URL/minecraft/profile/lookup/${UUIDSerializer.fromUUID(uuid)}")
            val status = response.status
            val name = runCatching { response.body<MojangResponse>().name }.getOrNull()
            return status to name
        }

        suspend fun getUuid(username: String): Pair<HttpStatusCode, UUID?> {
            val response = client.get("$BASE_URL/minecraft/profile/lookup/name/$username")
            val status = response.status
            val uuid = runCatching { response.body<MojangResponse>().id }.getOrNull()
            return status to uuid
        }
    }

    private object MinetoolsApi {
        private const val BASE_URL = "https://api.minetools.eu"

        suspend fun getUsername(uuid: UUID): Pair<HttpStatusCode, String?> {
            val response = client.get("$BASE_URL/uuid/${UUIDSerializer.fromUUID(uuid)}")
            val status = response.status
            val name = runCatching { response.body<MinetoolsResponse>().name }.getOrNull()
            return status to name
        }

        suspend fun getUuid(username: String): Pair<HttpStatusCode, UUID?> {
            val response = client.get("$BASE_URL/uuid/$username")
            val status = response.status
            val uuid = runCatching { response.body<MinetoolsResponse>().id }.getOrNull()
            return status to uuid
        }
    }

    /**
     * Response format for Mojang's API.
     * @property id Player UUID.
     * @property name Player username.
     */
    @Serializable
    private data class MojangResponse(
        val id: UUIDAsString,
        val name: String,
    )

    /**
     * Response format for Minetools API.
     * @property id Player UUID.
     * @property name Player username.
     */
    @Serializable
    private data class MinetoolsResponse(
        val id: UUIDAsString,
        val name: String,
    )
}

/** Type alias for UUID represented as a serializable string. */
private typealias UUIDAsString = @Serializable(with = UUIDSerializer::class) UUID

/**
 * Custom serializer for UUID, converting to and from simplified string format without dashes.
 */
private object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    private val uuidFormatRegex = "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})".toRegex()

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(fromUUID(value))
    }

    override fun deserialize(decoder: Decoder): UUID {
        return fromString(decoder.decodeString())
    }

    /** Converts UUID to string format without dashes. */
    fun fromUUID(value: UUID): String {
        return value.toString().replace("-", "")
    }

    /** Converts simplified UUID string back to standard UUID format. */
    fun fromString(input: String): UUID {
        return UUID.fromString(
            input.replaceFirst(uuidFormatRegex, "$1-$2-$3-$4-$5")
        )
    }
}