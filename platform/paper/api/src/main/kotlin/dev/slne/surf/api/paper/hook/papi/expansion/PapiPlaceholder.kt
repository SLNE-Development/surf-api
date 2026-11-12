package dev.slne.surf.api.paper.hook.papi.expansion

import org.bukkit.OfflinePlayer

abstract class PapiPlaceholder(
    val placeholder: String,
) {
    init {
        require(!placeholder.contains("%") && !placeholder.contains("{") && !placeholder.contains("_")) {
            "Placeholder may not contain %, {} or _"
        }
    }

    abstract fun parse(player: OfflinePlayer, args: List<String>): String?
}