package dev.slne.surf.api.shared.internal.nms

import kotlinx.serialization.json.Json

object NmsProviderConfig {
    const val NMS_PROVIDERS_DIRECTORY = "META-INF/surf-api/nms-providers"
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        prettyPrintIndent = "  "
    }
}