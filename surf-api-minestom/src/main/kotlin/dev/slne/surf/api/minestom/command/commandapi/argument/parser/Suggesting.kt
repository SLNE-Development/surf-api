package dev.slne.surf.api.minestom.command.commandapi.argument.parser

import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import java.util.concurrent.CompletableFuture

/**
 * Adds every entry of [values] that continues what has been typed so far, and completes the builder.
 *
 * Matching is case-insensitive because a suggestion is only a typing convenience; the parser still
 * decides which spellings it accepts.
 */
internal fun SuggestionsBuilder.suggestMatching(
    values: Iterable<String>,
): CompletableFuture<Suggestions> {
    values.forEach { value ->
        if (value.startsWith(remaining, ignoreCase = true)) suggest(value)
    }
    return buildFuture()
}
