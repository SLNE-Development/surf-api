package dev.slne.surf.api.core.server.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class StoreInfoResponse(
    @SerialName("store_key")
    val storeKey: String,

    @SerialName("supported_locales")
    val supportedLocaleStrings: List<String>
) {
    val supportedLocales
        get() = supportedLocaleStrings.map(Locale::forLanguageTag)
}
