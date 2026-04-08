package dev.slne.surf.api.core.server.messages

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.messages.translation.TranslationProcessor
import dev.slne.surf.api.core.util.checkInstantiationByServiceLoader
import net.kyori.adventure.key.Key
import java.util.*

@AutoService(TranslationProcessor::class)
class TranslationProcessorImpl : TranslationProcessor {
    init {
        checkInstantiationByServiceLoader()
    }

    private val referenceRegex = Regex("""\{reference:([^}]+)}""")
    private val placeholderRegex = Regex("""\{([^}:]+)}""")

    override fun process(
        storeKey: Key,
        locale: Locale,
        input: String,
        resolver: (String, Locale) -> String?,
        params: Map<String, Any>
    ): String {
        // First pass: resolve {reference:path} tokens (single-pass replacement)
        val referencesResolved = referenceRegex.replace(input) { match ->
            val path = placeholderRegex.replace(match.groupValues[1]) { innerMatch ->
                params[innerMatch.groupValues[1]]?.toString() ?: innerMatch.value
            }

            resolver(path, locale) ?: match.value
        }

        // Second pass: resolve {placeholder} tokens (single-pass replacement)
        return placeholderRegex.replace(referencesResolved) { match ->
            params[match.groupValues[1]]?.toString() ?: match.value
        }
    }
}