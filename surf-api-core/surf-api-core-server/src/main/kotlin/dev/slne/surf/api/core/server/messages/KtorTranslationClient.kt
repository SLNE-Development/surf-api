package dev.slne.surf.api.core.server.messages

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.messages.translation.TranslationClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import net.kyori.adventure.key.Key
import java.util.*

@AutoService(TranslationClient::class)
class KtorTranslationClient : TranslationClient {
    override suspend fun fetchAvailableLocales(key: Key): List<Locale> {
        val response = KtorClient.client
            .get("https://api.castcrafter.de/lang/${key.asString()}")
            .body<StoreInfoResponse>()

        return response.supportedLocales
    }

    override suspend fun fetchTranslations(
        key: Key,
        locale: Locale
    ): Map<String, Any> {
        val response = KtorClient.client
            .get("https://api.castcrafter.de/lang/${key.asString()}/${locale}")
            .body<Map<String, Any>>()

        return response
    }
}