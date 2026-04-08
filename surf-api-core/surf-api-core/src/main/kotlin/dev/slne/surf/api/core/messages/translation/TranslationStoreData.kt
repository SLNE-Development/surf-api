package dev.slne.surf.api.core.messages.translation

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import org.jetbrains.annotations.UnmodifiableView
import java.util.*

@InternalSurfApi
interface TranslationStoreData {
    val locales: @UnmodifiableView Object2ObjectMap<Locale, Map<String, Any>>
}
