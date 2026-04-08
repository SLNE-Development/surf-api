package dev.slne.surf.api.core.messages.translation

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import net.kyori.adventure.key.Key
import java.util.*

@InternalSurfApi
interface TranslationProcessor {
    fun process(
        storeKey: Key,
        locale: Locale,
        input: String,
        resolver: (String, Locale) -> String?,
        params: Map<String, Any>
    ): String

    companion object : TranslationProcessor by processor {
        val INSTANCE get() = processor
    }
}

private val processor = requiredService<TranslationProcessor>()