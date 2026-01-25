package dev.slne.surf.surfapi.shared.internal.hook

import kotlinx.serialization.json.Json

object HooksConfig {
    const val HOOKS_FILE_NAME = "surf-hooks.json"
    val json = Json {
        prettyPrint = true
        encodeDefaults = false
    }
}