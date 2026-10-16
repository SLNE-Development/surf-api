package dev.slne.surf.api.minestom.command.dsl

import dev.slne.surf.api.minestom.command.argument.Argument
import dev.slne.surf.api.minestom.command.suggestion.*

fun <T> Argument<T>.replaceSuggestions(vararg values: String): Argument<T> =
    replaceSuggestions(ArgumentSuggestions.strings(*values))

/**
 * Replaces the built-in suggestions with the strings [provider] returns for each request.
 *
 * A lambda literal passed directly resolves to the member [Argument.replaceSuggestions] instead of
 * this extension; pass a value explicitly typed `(SuggestionInfo) -> Collection<String>` to select
 * it.
 */
fun <T> Argument<T>.replaceSuggestions(provider: (SuggestionInfo) -> Collection<String>): Argument<T> =
    replaceSuggestions(ArgumentSuggestions.strings(provider))

fun <T> Argument<T>.includeSuggestions(vararg values: String): Argument<T> =
    includeSuggestions(ArgumentSuggestions.strings(*values))

/**
 * Adds the strings [provider] returns for each request to the built-in suggestions.
 *
 * A lambda literal passed directly resolves to the member [Argument.includeSuggestions] instead of
 * this extension; pass a value explicitly typed `(SuggestionInfo) -> Collection<String>` to select
 * it.
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
 * Replaces the built-in suggestions with the values [provider] returns for each request.
 *
 * A lambda literal passed directly resolves to the member [Argument.replaceSafeSuggestions]
 * instead of this extension; pass a value explicitly typed `(SuggestionInfo) -> Collection<T>` to
 * select it.
 */
fun <T> Argument<T>.replaceSafeSuggestions(provider: (SuggestionInfo) -> Collection<T>): Argument<T> =
    replaceSafeSuggestions(SafeSuggestions.suggest(provider))

fun <T> Argument<T>.includeSafeSuggestions(vararg values: T): Argument<T> =
    includeSafeSuggestions(SafeSuggestions.suggest(*values))

/**
 * Adds the values [provider] returns for each request to the built-in suggestions.
 *
 * A lambda literal passed directly resolves to the member [Argument.includeSafeSuggestions]
 * instead of this extension; pass a value explicitly typed `(SuggestionInfo) -> Collection<T>` to
 * select it.
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
