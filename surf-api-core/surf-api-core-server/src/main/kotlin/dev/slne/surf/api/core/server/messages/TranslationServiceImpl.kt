package dev.slne.surf.api.core.server.messages

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.messages.translation.TranslationProcessor
import dev.slne.surf.api.core.messages.translation.TranslationService
import dev.slne.surf.api.core.messages.translation.TranslationStoreData
import dev.slne.surf.api.core.messages.translation.TranslationStoreProvider
import dev.slne.surf.api.core.util.checkInstantiationByServiceLoader
import dev.slne.surf.api.core.util.logger
import net.kyori.adventure.key.Key
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@AutoService(TranslationService::class)
class TranslationServiceImpl : TranslationService {
    init {
        checkInstantiationByServiceLoader()
    }

    override fun translate(
        storeKey: Key,
        locale: Locale,
        key: String,
        params: Map<String, Any>
    ): String? {
        val store = TranslationStoreProvider.getStore(storeKey)

        if (store == null) {
            log.atWarning().log(
                "Translation store '%s' is not registered. Requested key: '%s'",
                storeKey, key
            )
            return null
        }

        val result = resolveFromStore(store, storeKey, locale, key, params)

        if (result == null) {
            log.atWarning().log(
                "Translation key '%s' is not registered in store '%s' for locale '%s'",
                key, storeKey, locale
            )
        }

        return result
    }

    override fun translateList(
        storeKey: Key,
        locale: Locale,
        key: String,
        params: Map<String, Any>
    ): List<String> {
        val store = TranslationStoreProvider.getStore(storeKey)

        if (store == null) {
            log.atWarning().log(
                "Translation store '%s' is not registered. Requested list key: '%s'",
                storeKey, key
            )
            return emptyList()
        }

        return buildList {
            for (i in 0..<MAX_LIST_ENTRIES) {
                val value = resolveFromStore(store, storeKey, locale, "$key.$i", params)
                    ?: break

                add(value)
            }
        }
    }

    /**
     * Resolves a translation key from a store, trying the locale fallback chain:
     * `requested locale` → `language-only locale` → `default locale` → `English`.
     */
    private fun resolveFromStore(
        store: TranslationStoreData,
        storeKey: Key,
        locale: Locale,
        key: String,
        params: Map<String, Any>
    ): String? {
        for (fallback in localeChain(locale)) {
            val map = store.locales[fallback] ?: continue
            val value = map[key] ?: continue

            fun resolve(path: String, loc: Locale): String? {
                for (fb in localeChain(loc)) {
                    val fbMap = store.locales[fb] ?: continue
                    val fbValue = fbMap[path] ?: continue

                    return when (fbValue) {
                        is String -> TranslationProcessor.process(storeKey, fb, fbValue, ::resolve, params)
                        else -> fbValue.toString()
                    }
                }
                return null
            }

            return when (value) {
                is String -> TranslationProcessor.process(storeKey, fallback, value, ::resolve, params)
                else -> value.toString()
            }
        }

        return null
    }

    companion object {
        private val log = logger()

        /** Maximum number of list entries to look up for [translateList]. */
        private const val MAX_LIST_ENTRIES = 50

        /** Cache for computed locale fallback chains to avoid re-computation. */
        private val localeChainCache = ConcurrentHashMap<Locale, List<Locale>>()

        /**
         * Builds a locale fallback chain for the given [locale].
         *
         * Example: `de_DE` → `[de_DE, de, <system-default>, en]`
         */
        private fun localeChain(locale: Locale): List<Locale> =
            localeChainCache.computeIfAbsent(locale) { loc ->
                buildList {
                    add(loc)

                    // Add language-only fallback (e.g. de_DE → de)
                    if (loc.country.isNotEmpty()) {
                        val languageOnly = Locale.of(loc.language)
                        if (languageOnly !in this) add(languageOnly)
                    }

                    // Add system default locale
                    val default = Locale.getDefault()
                    if (default !in this) {
                        add(default)
                        if (default.country.isNotEmpty()) {
                            val defaultLang = Locale.of(default.language)
                            if (defaultLang !in this) add(defaultLang)
                        }
                    }

                    // Ultimate fallback: English
                    if (Locale.ENGLISH !in this) add(Locale.ENGLISH)
                }
            }
    }
}