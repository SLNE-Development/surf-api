package dev.slne.surf.api.core.messages.translation

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import net.kyori.adventure.key.Key
import java.util.*

@InternalSurfApi
interface TranslationService {
    fun translate(
        storeKey: Key,
        locale: Locale,
        key: String,
        params: Map<String, Any> = emptyMap()
    ): String?

    fun translateList(
        storeKey: Key,
        locale: Locale,
        key: String,
        params: Map<String, Any> = emptyMap()
    ): List<String>

    companion object : TranslationService by provider {
        val INSTANCE get() = provider
    }
}

private val provider = requiredService<TranslationService>()