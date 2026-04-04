package dev.slne.surf.api.shared.internal.hook

import kotlinx.serialization.json.Json

object ComponentsConfig {
    const val COMPONENTS_DIRECTORY = "META-INF/surf-api/components"
    val json = Json {
        prettyPrint = true
        encodeDefaults = false
        ignoreUnknownKeys = true
        prettyPrintIndent = "  "
    }
}