package dev.slne.surf.api.core.server.messages

import dev.slne.surf.api.core.messages.translation.TranslationStoreData
import dev.slne.surf.api.core.util.mutableObject2ObjectMapOf
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import org.jetbrains.annotations.UnmodifiableView
import java.util.*

data class TranslationStoreDataImpl(
    override val locales: @UnmodifiableView Object2ObjectMap<Locale, Map<String, Any>> = mutableObject2ObjectMapOf()
) : TranslationStoreData
