package dev.slne.surf.api.core.server.util

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.retrooper.packetevents.protocol.player.TextureProperty
import com.google.gson.JsonParser
import com.sksamuel.aedile.core.asLoadingCache
import com.sksamuel.aedile.core.expireAfterWrite
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import java.io.IOException
import java.util.*
import kotlin.time.Duration.Companion.minutes

object PlayerSkinFetcher {
    private val client = OkHttpClient.Builder().build()
    private val SKIN_CACHE =
        Caffeine.newBuilder()
            .expireAfterWrite(10.minutes)
            .asLoadingCache<UUID, List<TextureProperty>> { fetchSkin0(it) }

    suspend fun fetchSkin(playerUuid: UUID) = SKIN_CACHE.get(playerUuid)

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun fetchSkin0(uuid: UUID): List<TextureProperty> {
        val request = Request.Builder()
            .url("https://sessionserver.mojang.com/session/minecraft/profile/$uuid?unsigned=false")
            .build()

        client.newCall(request).executeAsync().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Mojang session server returned HTTP ${response.code} for $uuid")
            }

            val jsonObject = JsonParser.parseReader(response.body.charStream()).asJsonObject
            val properties = jsonObject.getAsJsonArray("properties")
            return buildList(properties.size()) {
                for (property in properties) {
                    val json = property.asJsonObject
                    add(
                        TextureProperty(
                            json.get("name").asString,
                            json.get("value").asString,
                            json.get("signature")?.asString
                        )
                    )
                }
            }
        }
    }
}
