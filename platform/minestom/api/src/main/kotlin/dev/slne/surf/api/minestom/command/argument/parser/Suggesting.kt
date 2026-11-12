package dev.slne.surf.api.minestom.command.argument.parser

import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import java.util.concurrent.CompletableFuture

/**
 * Suggests every entry of [values] that starts with the remaining input, ignoring case, and builds
 * the result.
 */
internal fun SuggestionsBuilder.suggestMatching(
    values: Iterable<String>,
): CompletableFuture<Suggestions> {
    values.forEach { value ->
        if (value.startsWith(remaining, ignoreCase = true)) suggest(value)
    }
    return buildFuture()
}
