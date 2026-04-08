package dev.slne.surf.api.core.server.messages

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.messages.translation.TranslationClient
import dev.slne.surf.api.core.messages.translation.TranslationStoreData
import dev.slne.surf.api.core.messages.translation.TranslationStoreProvider
import dev.slne.surf.api.core.util.checkInstantiationByServiceLoader
import dev.slne.surf.api.core.util.freeze
import dev.slne.surf.api.core.util.mutableObject2ObjectMapOf
import dev.slne.surf.api.core.util.synchronize
import net.kyori.adventure.key.Key

@AutoService(TranslationStoreProvider::class)
class TranslationStoreProviderImpl : TranslationStoreProvider {
    init {
        checkInstantiationByServiceLoader()
    }
    private val _stores = mutableObject2ObjectMapOf<Key, TranslationStoreData>().synchronize()
    override val stores get() = _stores.freeze()

    override suspend fun loadStore(key: Key) {
        val store = TranslationStoreDataImpl()
        val locales = TranslationClient.fetchAvailableLocales(key)

        for (locale in locales) {
            val raw = TranslationClient.fetchTranslations(key, locale)
            val flat = TranslationFlattener.flatten(raw)

            store.locales[locale] = flat
        }

        _stores[key] = store
    }

    override fun getStore(key: Key): TranslationStoreData? {
        return _stores[key]
    }

    override fun isLoaded(key: Key): Boolean {
        return _stores.containsKey(key)
    }
}