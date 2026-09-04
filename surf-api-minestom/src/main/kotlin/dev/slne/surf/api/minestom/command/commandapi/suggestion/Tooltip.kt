package dev.slne.surf.api.minestom.command.commandapi.suggestion

import dev.slne.surf.api.minestom.command.commandapi.executor.CommandArguments
import net.kyori.adventure.text.Component
import net.minestom.server.command.CommandSender

data class Tooltip<T>(
    val suggestion: T,
    val tooltip: Component?,
)

data class StringTooltip(
    val suggestion: String,
    val tooltip: Component?,
) {
    companion object {
        fun ofString(suggestion: String, tooltip: Component? = null) =
            StringTooltip(suggestion, tooltip)
    }
}

data class SuggestionInfo(
    val sender: CommandSender,
    val previousArgs: CommandArguments,
    val currentInput: String,
    val currentArg: String,
)

enum class SuggestionFilter {
    PREFIX,
    NONE,
}
