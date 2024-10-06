package dev.slne.surf.surfapi.bukkit.api.hook.papi.expansion

import org.bukkit.OfflinePlayer

abstract class PapiPlaceholder(
    val placeholder: String,
) {
    abstract fun parse(player: OfflinePlayer, args: List<String>): String?
}