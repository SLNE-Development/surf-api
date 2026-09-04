package dev.slne.surf.api.minestom.command.commandapi.dsl

import dev.slne.surf.api.minestom.command.commandapi.argument.Argument
import dev.slne.surf.api.minestom.command.commandapi.suggestion.ArgumentSuggestions
import dev.slne.surf.api.minestom.command.commandapi.suggestion.SafeSuggestions
import dev.slne.surf.api.minestom.command.commandapi.suggestion.StringTooltip
import dev.slne.surf.api.minestom.command.commandapi.suggestion.SuggestionInfo
import dev.slne.surf.api.minestom.command.commandapi.suggestion.SuggestionsBranch
import dev.slne.surf.api.minestom.command.commandapi.suggestion.Tooltip

fun <T> Argument<T>.replaceSuggestions(vararg values: String): Argument<T> =
    replaceSuggestions(ArgumentSuggestions.strings(*values))

/**
 * A bare lambda literal at this call site resolves to [Argument.replaceSuggestions] instead of
 * this overload; pass an explicitly typed `(SuggestionInfo) -> Collection<String>` value to
 * select it.
 */
fun <T> Argument<T>.replaceSuggestions(provider: (SuggestionInfo) -> Collection<String>): Argument<T> =
    replaceSuggestions(ArgumentSuggestions.strings(provider))

fun <T> Argument<T>.includeSuggestions(vararg values: String): Argument<T> =
    includeSuggestions(ArgumentSuggestions.strings(*values))

/**
 * A bare lambda literal at this call site resolves to [Argument.includeSuggestions] instead of
 * this overload; pass an explicitly typed `(SuggestionInfo) -> Collection<String>` value to
 * select it.
 */
fun <T> Argument<T>.includeSuggestions(provider: (SuggestionInfo) -> Collection<String>): Argument<T> =
    includeSuggestions(ArgumentSuggestions.strings(provider))

fun <T> Argument<T>.replaceSuggestionsAsync(
    provider: suspend (SuggestionInfo) -> Collection<String>,
): Argument<T> = replaceSuggestions(ArgumentSuggestions.stringsAsync(provider))

fun <T> Argument<T>.includeSuggestionsAsync(
    provider: suspend (SuggestionInfo) -> Collection<String>,
): Argument<T> = includeSuggestions(ArgumentSuggestions.stringsAsync(provider))

fun <T> Argument<T>.replaceSuggestionsWithTooltips(vararg values: StringTooltip): Argument<T> =
    replaceSuggestions(ArgumentSuggestions.stringsWithTooltips(*values))

fun <T> Argument<T>.replaceSuggestionsWithTooltips(
    provider: (SuggestionInfo) -> Collection<StringTooltip>,
): Argument<T> = replaceSuggestions(ArgumentSuggestions.stringsWithTooltips(provider))

fun <T> Argument<T>.includeSuggestionsWithTooltips(vararg values: StringTooltip): Argument<T> =
    includeSuggestions(ArgumentSuggestions.stringsWithTooltips(*values))

fun <T> Argument<T>.includeSuggestionsWithTooltips(
    provider: (SuggestionInfo) -> Collection<StringTooltip>,
): Argument<T> = includeSuggestions(ArgumentSuggestions.stringsWithTooltips(provider))

fun <T> Argument<T>.replaceSuggestionsWithTooltipsAsync(
    provider: suspend (SuggestionInfo) -> Collection<StringTooltip>,
): Argument<T> = replaceSuggestions(ArgumentSuggestions.stringsWithTooltipsAsync(provider))

fun <T> Argument<T>.includeSuggestionsWithTooltipsAsync(
    provider: suspend (SuggestionInfo) -> Collection<StringTooltip>,
): Argument<T> = includeSuggestions(ArgumentSuggestions.stringsWithTooltipsAsync(provider))

fun <T> Argument<T>.replaceSafeSuggestions(vararg values: T): Argument<T> =
    replaceSafeSuggestions(SafeSuggestions.suggest(*values))

/**
 * A bare lambda literal at this call site resolves to [Argument.replaceSafeSuggestions] instead
 * of this overload; pass an explicitly typed `(SuggestionInfo) -> Collection<T>` value to select
 * it.
 */
fun <T> Argument<T>.replaceSafeSuggestions(provider: (SuggestionInfo) -> Collection<T>): Argument<T> =
    replaceSafeSuggestions(SafeSuggestions.suggest(provider))

fun <T> Argument<T>.includeSafeSuggestions(vararg values: T): Argument<T> =
    includeSafeSuggestions(SafeSuggestions.suggest(*values))

/**
 * A bare lambda literal at this call site resolves to [Argument.includeSafeSuggestions] instead
 * of this overload; pass an explicitly typed `(SuggestionInfo) -> Collection<T>` value to select
 * it.
 */
fun <T> Argument<T>.includeSafeSuggestions(provider: (SuggestionInfo) -> Collection<T>): Argument<T> =
    includeSafeSuggestions(SafeSuggestions.suggest(provider))

fun <T> Argument<T>.replaceSafeSuggestionsAsync(
    provider: suspend (SuggestionInfo) -> Collection<T>,
): Argument<T> = replaceSafeSuggestions(SafeSuggestions.suggestAsync(provider))

fun <T> Argument<T>.includeSafeSuggestionsAsync(
    provider: suspend (SuggestionInfo) -> Collection<T>,
): Argument<T> = includeSafeSuggestions(SafeSuggestions.suggestAsync(provider))

fun <T> Argument<T>.replaceSafeSuggestionsWithTooltips(vararg values: Tooltip<T>): Argument<T> =
    replaceSafeSuggestions(SafeSuggestions.tooltips(*values))

fun <T> Argument<T>.replaceSafeSuggestionsWithTooltips(
    provider: (SuggestionInfo) -> Collection<Tooltip<T>>,
): Argument<T> = replaceSafeSuggestions(SafeSuggestions.tooltips(provider))

fun <T> Argument<T>.includeSafeSuggestionsWithTooltips(vararg values: Tooltip<T>): Argument<T> =
    includeSafeSuggestions(SafeSuggestions.tooltips(*values))

fun <T> Argument<T>.includeSafeSuggestionsWithTooltips(
    provider: (SuggestionInfo) -> Collection<Tooltip<T>>,
): Argument<T> = includeSafeSuggestions(SafeSuggestions.tooltips(provider))

fun <T> Argument<T>.replaceSafeSuggestionsWithTooltipsAsync(
    provider: suspend (SuggestionInfo) -> Collection<Tooltip<T>>,
): Argument<T> = replaceSafeSuggestions(SafeSuggestions.tooltipsAsync(provider))

fun <T> Argument<T>.includeSafeSuggestionsWithTooltipsAsync(
    provider: suspend (SuggestionInfo) -> Collection<Tooltip<T>>,
): Argument<T> = includeSafeSuggestions(SafeSuggestions.tooltipsAsync(provider))

fun <T> Argument<T>.replaceSuggestions(branch: SuggestionsBranch): Argument<T> =
    replaceSuggestions(ArgumentSuggestions.branch(branch))

fun <T> Argument<T>.includeSuggestions(branch: SuggestionsBranch): Argument<T> =
    includeSuggestions(ArgumentSuggestions.branch(branch))
