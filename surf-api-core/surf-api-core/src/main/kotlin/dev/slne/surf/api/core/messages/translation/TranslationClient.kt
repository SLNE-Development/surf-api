package dev.slne.surf.api.core.messages.translation

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import net.kyori.adventure.key.Key
import java.util.*

@InternalSurfApi
interface TranslationClient {
    suspend fun fetchAvailableLocales(key: Key): List<Locale>

    suspend fun fetchTranslations(
        key: Key,
        locale: Locale
    ): Map<String, Any>

    companion object : TranslationClient by client {
        val INSTANCE get() = client
    }
}

private val client = requiredService<TranslationClient>()