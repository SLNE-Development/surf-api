package dev.slne.surf.surfapi.core.api.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asLoadingCache
import com.sksamuel.aedile.core.expireAfterWrite
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
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
     * Cache mapping usernames to UUIDs.
     * Cached entries expire after 15 minutes.
     */
    private val nameToUuid = Caffeine.newBuilder()
        .expireAfterWrite(15.minutes)
        .asLoadingCache<String, UUID?> {
            try {
                MinecraftApi.getUuid(it)
            } catch (_: Exception) {
                try {
                    MinetoolsApi.getUuid(it)
                } catch (_: Exception) {
                    null
                }
            }
        }

    /**
     * Cache mapping UUIDs to usernames.
     * Cached entries expire after 15 minutes.
     */
    private val uuidToName = Caffeine.newBuilder()
        .expireAfterWrite(15.minutes)
        .asLoadingCache<UUID, String?> {
            try {
                MinecraftApi.getUsername(it)
            } catch (_: Exception) {
                try {
                    MinetoolsApi.getUsername(it)
                } catch (_: Exception) {
                    null
                }
            }
        }

    /**
     * Retrieves the username corresponding to the provided UUID.
     * @param uuid UUID of the player.
     * @return Username associated with the UUID or null if not found.
     */
    suspend fun getUsername(uuid: UUID): String? = uuidToName.get(uuid)

    /**
     * Retrieves the UUID corresponding to the provided username.
     * @param username Minecraft username.
     * @return UUID associated with the username or null if not found.
     */
    suspend fun getUuid(username: String): UUID? = nameToUuid.get(username)

    /**
     * API interaction with Mojang's official endpoints.
     */
    private object MinecraftApi {
        private const val BASE_URL = "https://api.mojang.com"

        /**
         * Fetches username from Mojang using UUID.
         * @param uuid Player UUID.
         * @return Username retrieved from Mojang.
         */
        suspend fun getUsername(uuid: UUID): String {
            return client.get("$BASE_URL/user/profile/${UUIDSerializer.fromUUID(uuid)}")
                .body<MojangResponse>()
                .name
        }

        /**
         * Fetches UUID from Mojang using username.
         * @param username Player username.
         * @return UUID retrieved from Mojang.
         */
        suspend fun getUuid(username: String): UUID {
            return client.get("$BASE_URL/users/profiles/minecraft/$username")
                .body<MojangResponse>()
                .id
        }
    }

    /**
     * API interaction with Minetools as a fallback.
     */
    private object MinetoolsApi {
        private const val BASE_URL = "https://api.minetools.eu"

        /**
         * Fetches username from Minetools using UUID.
         * @param uuid Player UUID.
         * @return Username retrieved from Minetools.
         */
        suspend fun getUsername(uuid: UUID): String {
            return client.get("$BASE_URL/uuid/${UUIDSerializer.fromUUID(uuid)}")
                .body<MinetoolsResponse>()
                .name
        }

        /**
         * Fetches UUID from Minetools using username.
         * @param username Player username.
         * @return UUID retrieved from Minetools.
         */
        suspend fun getUuid(username: String): UUID {
            return client.get("$BASE_URL/uuid/$username")
                .body<MinetoolsResponse>()
                .id
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
typealias UUIDAsString = @Serializable(with = UUIDSerializer::class) UUID

/**
 * Custom serializer for UUID, converting to and from simplified string format without dashes.
 */
object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

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
            input.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})".toRegex(), "$1-$2-$3-$4-$5"
            )
        )
    }
}