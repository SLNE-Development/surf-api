package dev.slne.surf.surfapi.shared.internal.component

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
