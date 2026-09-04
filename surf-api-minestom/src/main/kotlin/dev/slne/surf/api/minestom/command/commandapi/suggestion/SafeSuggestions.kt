/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.surf.api.minestom.command.commandapi.suggestion

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectImmutableList
import it.unimi.dsi.fastutil.objects.ObjectList

fun interface SafeSuggestions<T> {
    suspend fun suggest(info: SuggestionInfo): List<Tooltip<T>>

    companion object {
        fun <T> empty(): SafeSuggestions<T> = SafeSuggestions { emptyList() }

        fun <T> suggest(vararg values: T): SafeSuggestions<T> = suggest(ObjectList.of(*values))

        fun <T> suggest(values: Collection<T>): SafeSuggestions<T> {
            val copy = ObjectImmutableList(values)
            return SafeSuggestions {
                copy.mapTo(ObjectArrayList(copy.size), ::withoutTooltip)
            }
        }

        fun <T> suggest(provider: (SuggestionInfo) -> Collection<T>): SafeSuggestions<T> {
            return SafeSuggestions { info ->
                val provided = provider(info)
                provided.mapTo(ObjectArrayList(provided.size), ::withoutTooltip)
            }
        }

        fun <T> suggestCollection(provider: (SuggestionInfo) -> Collection<T>): SafeSuggestions<T> =
            suggest(provider)

        fun <T> suggestAsync(provider: suspend (SuggestionInfo) -> Collection<T>): SafeSuggestions<T> {
            return SafeSuggestions { info ->
                val provided = provider(info)
                provided.mapTo(ObjectArrayList(provided.size), ::withoutTooltip)
            }
        }

        fun <T> suggestCollectionAsync(provider: suspend (SuggestionInfo) -> Collection<T>): SafeSuggestions<T> =
            suggestAsync(provider)

        fun <T> tooltips(vararg values: Tooltip<T>): SafeSuggestions<T> =
            tooltips(ObjectList.of(*values))

        fun <T> tooltips(values: Collection<Tooltip<T>>): SafeSuggestions<T> {
            val copy = ObjectImmutableList(values)
            return SafeSuggestions { copy }
        }

        fun <T> tooltips(provider: (SuggestionInfo) -> Collection<Tooltip<T>>): SafeSuggestions<T> {
            return SafeSuggestions { info -> ObjectImmutableList(provider(info)) }
        }

        fun <T> tooltipCollection(provider: (SuggestionInfo) -> Collection<Tooltip<T>>): SafeSuggestions<T> =
            tooltips(provider)

        fun <T> tooltipsAsync(provider: suspend (SuggestionInfo) -> Collection<Tooltip<T>>): SafeSuggestions<T> =
            SafeSuggestions { info -> ObjectImmutableList(provider(info)) }

        fun <T> tooltipCollectionAsync(provider: suspend (SuggestionInfo) -> Collection<Tooltip<T>>): SafeSuggestions<T> =
            tooltipsAsync(provider)

        private fun <T> withoutTooltip(value: T) = Tooltip(value, null)
    }
}
