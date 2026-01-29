@file:Suppress("UnstableApiUsage")

package dev.slne.surf.surfapi.bukkit.api.hook.papi.expansion

import dev.slne.surf.surfapi.bukkit.api.util.getCallingPlugin
import org.bukkit.OfflinePlayer

open class PapiExpansion @JvmOverloads constructor(
    val identifier: String,
    val placeholder: List<PapiPlaceholder>,
    val author: String = getCallingPlugin(2).pluginMeta.authors.joinToString(", "),
    val version: String = getCallingPlugin(2).pluginMeta.version,
    val name: String = identifier,
) {
    init {
        require(!identifier.contains("%") && !identifier.contains("{") && !identifier.contains("_")) { "Identifier may not contain %, {} or _" }
    }

    val placeholders = placeholder.associateBy { it.placeholder }

    open fun parseWithNoParams(player: OfflinePlayer): String? = null
}