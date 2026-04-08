package dev.slne.surf.api.core.messages.translation

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import net.kyori.adventure.key.Key
import org.jetbrains.annotations.UnmodifiableView

@InternalSurfApi
interface TranslationStoreProvider {
    val stores: @UnmodifiableView Object2ObjectMap<Key, TranslationStoreData>

    suspend fun loadStore(key: Key)
    fun getStore(key: Key): TranslationStoreData?
    fun isLoaded(key: Key): Boolean

    companion object : TranslationStoreProvider by provider {
        val INSTANCE get() = provider
    }
}

private val provider = requiredService<TranslationStoreProvider>()