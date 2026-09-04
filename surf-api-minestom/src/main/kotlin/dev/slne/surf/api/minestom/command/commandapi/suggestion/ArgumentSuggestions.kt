/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.surf.api.minestom.command.commandapi.suggestion

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectImmutableList
import it.unimi.dsi.fastutil.objects.ObjectList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

fun interface ArgumentSuggestions {
    suspend fun suggest(info: SuggestionInfo): List<StringTooltip>

    val filterPolicy: SuggestionFilter
        get() = SuggestionFilter.PREFIX

    companion object {
        fun empty(): ArgumentSuggestions = ArgumentSuggestions { emptyList() }

        fun strings(vararg values: String): ArgumentSuggestions = strings(ObjectList.of(*values))

        fun strings(values: Collection<String>): ArgumentSuggestions {
            val copy = ObjectImmutableList(values)
            return ArgumentSuggestions {
                copy.mapTo(ObjectArrayList(copy.size), ::withoutTooltip)
            }
        }

        fun strings(provider: (SuggestionInfo) -> Collection<String>): ArgumentSuggestions {
            return ArgumentSuggestions { info ->
                val provided = provider(info)
                provided.mapTo(ObjectArrayList(provided.size), ::withoutTooltip)
            }
        }

        fun stringCollection(provider: (SuggestionInfo) -> Collection<String>): ArgumentSuggestions {
            return strings(provider)
        }

        fun stringsAsync(provider: suspend (SuggestionInfo) -> Collection<String>): ArgumentSuggestions {
            return ArgumentSuggestions { info ->
                val provided = provider(info)
                provided.mapTo(ObjectArrayList(provided.size), ::withoutTooltip)
            }
        }

        fun stringCollectionAsync(provider: suspend (SuggestionInfo) -> Collection<String>): ArgumentSuggestions =
            stringsAsync(provider)

        fun stringsWithTooltips(vararg values: StringTooltip): ArgumentSuggestions =
            stringsWithTooltips(ObjectList.of(*values))

        fun stringsWithTooltips(values: Collection<StringTooltip>): ArgumentSuggestions {
            val copy = ObjectImmutableList(values)
            return ArgumentSuggestions { copy }
        }

        fun stringsWithTooltips(provider: (SuggestionInfo) -> Collection<StringTooltip>): ArgumentSuggestions {
            return ArgumentSuggestions { info -> ObjectImmutableList(provider(info)) }
        }

        fun stringsWithTooltipsCollection(provider: (SuggestionInfo) -> Collection<StringTooltip>): ArgumentSuggestions =
            stringsWithTooltips(provider)

        fun stringsWithTooltipsAsync(provider: suspend (SuggestionInfo) -> Collection<StringTooltip>): ArgumentSuggestions {
            return ArgumentSuggestions { info -> ObjectImmutableList(provider(info)) }
        }

        fun stringsWithTooltipsCollectionAsync(provider: suspend (SuggestionInfo) -> Collection<StringTooltip>): ArgumentSuggestions =
            stringsWithTooltipsAsync(provider)

        fun unfiltered(provider: suspend (SuggestionInfo) -> Collection<StringTooltip>): ArgumentSuggestions {
            return object : ArgumentSuggestions {
                override val filterPolicy = SuggestionFilter.NONE

                override suspend fun suggest(info: SuggestionInfo): List<StringTooltip> {
                    return ObjectImmutableList(provider(info))
                }
            }
        }

        fun merge(vararg providers: ArgumentSuggestions): ArgumentSuggestions {
            val copy = ObjectImmutableList(providers)
            return ArgumentSuggestions { info ->
                coroutineScope {
                    copy.mapTo(ObjectArrayList(copy.size)) { provider ->
                        async { provider.suggest(info) }
                    }
                        .awaitAll()
                        .flatten()
                }
            }
        }

        fun branch(branch: SuggestionsBranch): ArgumentSuggestions = ArgumentSuggestions { info ->
            branch.getNextSuggestion(info.sender, *info.previousArgs.rawArguments().toTypedArray())
                ?.suggest(info)
                ?.let(::ObjectImmutableList)
                .orEmpty()
        }

        private fun withoutTooltip(value: String) = StringTooltip(value, null)
    }
}
