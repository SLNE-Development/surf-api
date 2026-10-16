/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.surf.api.minestom.command.suggestion

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectImmutableList
import it.unimi.dsi.fastutil.objects.ObjectList

/**
 * Supplies suggestions as values of the argument's own type [T].
 *
 * The values are converted to text with the argument's formatter, so every suggestion is spelled
 * the way the argument parses it. Entries are always filtered by the typed prefix. Providers run
 * like [ArgumentSuggestions]: in a coroutine on the asynchronous scope, with exceptions logged.
 *
 * Register a provider with [dev.slne.surf.api.minestom.command.argument.Argument.replaceSafeSuggestions]
 * or [dev.slne.surf.api.minestom.command.argument.Argument.includeSafeSuggestions].
 */
fun interface SafeSuggestions<T> {
    /**
     * Returns the suggestions for the input described by [info].
     */
    suspend fun suggest(info: SuggestionInfo): List<Tooltip<T>>

    companion object {
        /**
         * Returns a provider without entries.
         */
        fun <T> empty(): SafeSuggestions<T> = SafeSuggestions { emptyList() }

        /**
         * Returns a provider that suggests the fixed [values].
         */
        fun <T> suggest(vararg values: T): SafeSuggestions<T> = suggest(ObjectList.of(*values))

        /**
         * Returns a provider that suggests the fixed [values].
         */
        fun <T> suggest(values: Collection<T>): SafeSuggestions<T> {
            val copy = ObjectImmutableList(values)
            return SafeSuggestions {
                copy.mapTo(ObjectArrayList(copy.size), ::withoutTooltip)
            }
        }

        /**
         * Returns a provider that suggests the values [provider] returns for each request.
         */
        fun <T> suggest(provider: (SuggestionInfo) -> Collection<T>): SafeSuggestions<T> {
            return SafeSuggestions { info ->
                val provided = provider(info)
                provided.mapTo(ObjectArrayList(provided.size), ::withoutTooltip)
            }
        }

        /**
         * Alias of [suggest] for a provider.
         */
        fun <T> suggestCollection(provider: (SuggestionInfo) -> Collection<T>): SafeSuggestions<T> =
            suggest(provider)

        /**
         * Returns a provider that suggests the values the suspending [provider] returns for each
         * request.
         */
        fun <T> suggestAsync(provider: suspend (SuggestionInfo) -> Collection<T>): SafeSuggestions<T> {
            return SafeSuggestions { info ->
                val provided = provider(info)
                provided.mapTo(ObjectArrayList(provided.size), ::withoutTooltip)
            }
        }

        /**
         * Alias of [suggestAsync].
         */
        fun <T> suggestCollectionAsync(provider: suspend (SuggestionInfo) -> Collection<T>): SafeSuggestions<T> =
            suggestAsync(provider)

        /**
         * Returns a provider that suggests the fixed [values] with their tooltips.
         */
        fun <T> tooltips(vararg values: Tooltip<T>): SafeSuggestions<T> =
            tooltips(ObjectList.of(*values))

        /**
         * Returns a provider that suggests the fixed [values] with their tooltips.
         */
        fun <T> tooltips(values: Collection<Tooltip<T>>): SafeSuggestions<T> {
            val copy = ObjectImmutableList(values)
            return SafeSuggestions { copy }
        }

        /**
         * Returns a provider that suggests the entries [provider] returns for each request.
         */
        fun <T> tooltips(provider: (SuggestionInfo) -> Collection<Tooltip<T>>): SafeSuggestions<T> {
            return SafeSuggestions { info -> ObjectImmutableList(provider(info)) }
        }

        /**
         * Alias of [tooltips] for a provider.
         */
        fun <T> tooltipCollection(provider: (SuggestionInfo) -> Collection<Tooltip<T>>): SafeSuggestions<T> =
            tooltips(provider)

        /**
         * Returns a provider that suggests the entries the suspending [provider] returns for each
         * request.
         */
        fun <T> tooltipsAsync(provider: suspend (SuggestionInfo) -> Collection<Tooltip<T>>): SafeSuggestions<T> =
            SafeSuggestions { info -> ObjectImmutableList(provider(info)) }

        /**
         * Alias of [tooltipsAsync].
         */
        fun <T> tooltipCollectionAsync(provider: suspend (SuggestionInfo) -> Collection<Tooltip<T>>): SafeSuggestions<T> =
            tooltipsAsync(provider)

        private fun <T> withoutTooltip(value: T) = Tooltip(value, null)
    }
}
