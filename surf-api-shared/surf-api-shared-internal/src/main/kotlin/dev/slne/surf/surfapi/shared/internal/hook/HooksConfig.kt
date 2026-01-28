package dev.slne.surf.surfapi.shared.internal.hook

import kotlinx.serialization.json.Json

object HooksConfig {
    const val HOOKS_DIRECTORY = "META-INF/surf-api/hooks"
    val json = Json {
        prettyPrint = true
        encodeDefaults = false
        ignoreUnknownKeys = true
        prettyPrintIndent = "  "
    }
}