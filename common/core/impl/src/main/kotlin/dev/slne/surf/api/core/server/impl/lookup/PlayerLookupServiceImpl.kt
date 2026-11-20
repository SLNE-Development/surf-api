package dev.slne.surf.api.core.server.impl.lookup

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import com.sksamuel.aedile.core.asLoadingCache
import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.core.service.PlayerLookupService
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.springframework.stereotype.Service
import java.util.*
import kotlin.time.Duration.Companion.minutes

@Service
internal class PlayerLookupServiceImpl(
    private val http: JsonHttp,
) : PlayerLookupService {

    private val nameToUuid = Caffeine.newBuilder()
        .expireAfter(LookupExpiry<String, UUID>())
        .recordStats()
        .maximumSize(CACHE_SIZE)
        .asLoadingCache { name -> lookupUuid(name) }

    private val uuidToName = Caffeine.newBuilder()
        .expireAfter(LookupExpiry<UUID, String>())
        .recordStats()
        .maximumSize(CACHE_SIZE)
        .asLoadingCache { uuid -> lookupUsername(uuid) }

    override suspend fun getUsername(uuid: UUID): String? = uuidToName.get(uuid).valueOrNull()

    override suspend fun getUuid(username: String): UUID? = nameToUuid.get(username).valueOrNull()

    override fun getCacheStats(): PlayerLookupService.CacheStats {
        val names = nameToUuid.underlying().synchronous()
        val uuids = uuidToName.underlying().synchronous()
        return PlayerLookupService.CacheStats(
            nameToUuidHitRate = names.stats().hitRate(),
            nameToUuidSize = names.estimatedSize(),
            uuidToNameHitRate = uuids.stats().hitRate(),
            uuidToNameSize = uuids.estimatedSize(),
        )
    }

    private suspend fun lookupUuid(name: String): LookupResult<UUID> {
        return lookup("UUID for username $name") {
            val segment = JsonHttp.pathSegment(name)
            firstFound(
                { http.get<ProfileResponse>("https://api.mojang.com/users/profiles/minecraft/$segment") },
                { http.get<ProfileResponse>("https://api.minecraftservices.com/minecraft/profile/lookup/name/$segment") },
                { http.get<ProfileResponse>("https://api.minetools.eu/uuid/$segment") },
            ) { it.id }
        }
    }

    private suspend fun lookupUsername(uuid: UUID): LookupResult<String> {
        return lookup("username for UUID $uuid") {
            val compact = UndashedUuidSerializer.format(uuid)
            firstFound(
                { http.get<ProfileResponse>("https://api.mojang.com/user/profile/$compact") },
                { http.get<ProfileResponse>("https://api.minecraftservices.com/minecraft/profile/lookup/$compact") },
                { http.get<ProfileResponse>("https://api.minetools.eu/uuid/$compact") },
            ) { it.name }
        }
    }

    /**
     * Asks [sources] in order and returns the first value found. Mojang's 404 is final: the other
     * sources mirror its data, so they are not asked about a player Mojang does not know.
     */
    private suspend inline fun <T> firstFound(
        vararg sources: suspend () -> JsonHttp.Response<ProfileResponse>,
        extract: (ProfileResponse) -> T,
    ): LookupResult<T> {
        var last: JsonHttp.Response<ProfileResponse>? = null
        for ((index, source) in sources.withIndex()) {
            val response = source()
            response.body?.takeIf { response.isSuccess }
                ?.let { return LookupResult.Found(extract(it)) }
            if (index == 0 && response.status == HTTP_NOT_FOUND) return LookupResult.NotFound
            last = response
        }
        return failureOf(checkNotNull(last).status)
    }

    private inline fun <T> lookup(subject: String, block: () -> LookupResult<T>): LookupResult<T> {
        return try {
            block()
        } catch (exception: Exception) {
            log.atWarning().withCause(exception).log("Failed to look up the %s", subject)
            LookupResult.Failed(exception)
        }
    }

    private fun <T> failureOf(status: Int): LookupResult<T> = when (status) {
        HTTP_NOT_FOUND -> LookupResult.NotFound
        HTTP_TOO_MANY_REQUESTS -> LookupResult.RateLimited("Rate limit exceeded")
        HTTP_UNAVAILABLE -> LookupResult.RateLimited("Service unavailable")
        HTTP_GATEWAY_TIMEOUT -> LookupResult.RateLimited("Timeout")
        else -> LookupResult.RateLimited("API error: $status")
    }

    /**
     * The outcome of one lookup, cached as is so that a missing player or a failure is not asked
     * for again until its entry expires.
     */
    private sealed interface LookupResult<out T> {
        data class Found<T>(val value: T) : LookupResult<T>
        data object NotFound : LookupResult<Nothing>
        data class RateLimited(val reason: String) : LookupResult<Nothing>
        data class Failed(val error: Throwable) : LookupResult<Nothing>
    }

    private fun <T> LookupResult<T>.valueOrNull(): T? = (this as? LookupResult.Found)?.value

    /**
     * Keeps found players for 15 minutes, unknown ones for 5 and failures for 1.
     */
    private class LookupExpiry<K : Any, V> : Expiry<K, LookupResult<V>> {
        override fun expireAfterCreate(key: K, value: LookupResult<V>, currentTime: Long): Long =
            when (value) {
                is LookupResult.Found -> 15.minutes.inWholeNanoseconds
                is LookupResult.NotFound -> 5.minutes.inWholeNanoseconds
                is LookupResult.RateLimited, is LookupResult.Failed -> 1.minutes.inWholeNanoseconds
            }

        override fun expireAfterUpdate(
            key: K,
            value: LookupResult<V>,
            currentTime: Long,
            currentDuration: Long
        ) =
            expireAfterCreate(key, value, currentTime)

        override fun expireAfterRead(
            key: K,
            value: LookupResult<V>,
            currentTime: Long,
            currentDuration: Long
        ) =
            currentDuration
    }

    /**
     * The profile all three APIs return; Mojang and Minetools both name the fields `id` and `name`.
     */
    @Serializable
    private data class ProfileResponse(
        @Serializable(with = UndashedUuidSerializer::class) val id: UUID,
        val name: String,
    )

    /**
     * Reads and writes UUIDs without dashes, as the profile APIs use them.
     */
    private object UndashedUuidSerializer : KSerializer<UUID> {
        override val descriptor = PrimitiveSerialDescriptor("UndashedUuid", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(format(value))

        override fun deserialize(decoder: Decoder): UUID = parse(decoder.decodeString())

        fun format(uuid: UUID): String = uuid.toString().replace("-", "")

        private fun parse(value: String): UUID = UUID.fromString(
            if ('-' in value) value
            else "${value.substring(0, 8)}-${value.substring(8, 12)}-${value.substring(12, 16)}-" +
                    "${value.substring(16, 20)}-${value.substring(20)}"
        )
    }

    private companion object {
        val log = logger()

        const val CACHE_SIZE = 10_000L
        const val HTTP_NOT_FOUND = 404
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val HTTP_UNAVAILABLE = 503
        const val HTTP_GATEWAY_TIMEOUT = 504
    }
}
