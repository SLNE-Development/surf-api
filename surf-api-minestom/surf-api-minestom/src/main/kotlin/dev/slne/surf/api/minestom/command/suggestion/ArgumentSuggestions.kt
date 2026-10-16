/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.surf.api.minestom.command.suggestion

import dev.slne.surf.api.minestom.command.exception.CommandSyntaxException
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectImmutableList
import it.unimi.dsi.fastutil.objects.ObjectList
import kotlinx.coroutines.*
import net.kyori.adventure.text.logger.slf4j.ComponentLogger

/**
 * Supplies suggestions for an argument.
 *
 * Providers run in a coroutine on the server's asynchronous scope, not on the network thread, and
 * may suspend. An exception thrown by a provider is logged and the provider contributes no entries.
 *
 * Register a provider with [dev.slne.surf.api.minestom.command.argument.Argument.replaceSuggestions]
 * or [dev.slne.surf.api.minestom.command.argument.Argument.includeSuggestions].
 */
fun interface ArgumentSuggestions {
    /**
     * Returns the suggestions for the input described by [info].
     */
    suspend fun suggest(info: SuggestionInfo): List<StringTooltip>

    /**
     * How the returned entries are matched against the text typed for the argument.
     */
    val filterPolicy: SuggestionFilter
        get() = SuggestionFilter.PREFIX

    companion object {
        /**
         * Returns a provider without entries.
         */
        fun empty(): ArgumentSuggestions = ArgumentSuggestions { emptyList() }

        /**
         * Returns a provider that suggests the fixed [values].
         */
        fun strings(vararg values: String): ArgumentSuggestions = strings(ObjectList.of(*values))

        /**
         * Returns a provider that suggests the fixed [values].
         */
        fun strings(values: Collection<String>): ArgumentSuggestions {
            val copy = ObjectImmutableList(values)
            return ArgumentSuggestions {
                copy.mapTo(ObjectArrayList(copy.size), ::withoutTooltip)
            }
        }

        /**
         * Returns a provider that suggests the strings [provider] returns for each request.
         */
        fun strings(provider: (SuggestionInfo) -> Collection<String>): ArgumentSuggestions {
            return ArgumentSuggestions { info ->
                val provided = provider(info)
                provided.mapTo(ObjectArrayList(provided.size), ::withoutTooltip)
            }
        }

        /**
         * Alias of [strings] for a provider.
         */
        fun stringCollection(provider: (SuggestionInfo) -> Collection<String>): ArgumentSuggestions {
            return strings(provider)
        }

        /**
         * Returns a provider that suggests the strings the suspending [provider] returns for each
         * request.
         */
        fun stringsAsync(provider: suspend (SuggestionInfo) -> Collection<String>): ArgumentSuggestions {
            return ArgumentSuggestions { info ->
                val provided = provider(info)
                provided.mapTo(ObjectArrayList(provided.size), ::withoutTooltip)
            }
        }

        /**
         * Alias of [stringsAsync].
         */
        fun stringCollectionAsync(provider: suspend (SuggestionInfo) -> Collection<String>): ArgumentSuggestions =
            stringsAsync(provider)

        /**
         * Returns a provider that suggests the fixed [values] with their tooltips.
         */
        fun stringsWithTooltips(vararg values: StringTooltip): ArgumentSuggestions =
            stringsWithTooltips(ObjectList.of(*values))

        /**
         * Returns a provider that suggests the fixed [values] with their tooltips.
         */
        fun stringsWithTooltips(values: Collection<StringTooltip>): ArgumentSuggestions {
            val copy = ObjectImmutableList(values)
            return ArgumentSuggestions { copy }
        }

        /**
         * Returns a provider that suggests the entries [provider] returns for each request.
         */
        fun stringsWithTooltips(provider: (SuggestionInfo) -> Collection<StringTooltip>): ArgumentSuggestions {
            return ArgumentSuggestions { info -> ObjectImmutableList(provider(info)) }
        }

        /**
         * Alias of [stringsWithTooltips] for a provider.
         */
        fun stringsWithTooltipsCollection(provider: (SuggestionInfo) -> Collection<StringTooltip>): ArgumentSuggestions =
            stringsWithTooltips(provider)

        /**
         * Returns a provider that suggests the entries the suspending [provider] returns for each
         * request.
         */
        fun stringsWithTooltipsAsync(provider: suspend (SuggestionInfo) -> Collection<StringTooltip>): ArgumentSuggestions {
            return ArgumentSuggestions { info -> ObjectImmutableList(provider(info)) }
        }

        /**
         * Alias of [stringsWithTooltipsAsync].
         */
        fun stringsWithTooltipsCollectionAsync(provider: suspend (SuggestionInfo) -> Collection<StringTooltip>): ArgumentSuggestions =
            stringsWithTooltipsAsync(provider)

        /**
         * Returns a provider whose entries are offered without prefix filtering
         * ([SuggestionFilter.NONE]).
         */
        fun unfiltered(provider: suspend (SuggestionInfo) -> Collection<StringTooltip>): ArgumentSuggestions {
            return object : ArgumentSuggestions {
                override val filterPolicy = SuggestionFilter.NONE

                override suspend fun suggest(info: SuggestionInfo): List<StringTooltip> {
                    return ObjectImmutableList(provider(info))
                }
            }
        }

        /**
         * Returns a provider that concatenates the entries of all [providers], which run
         * concurrently.
         *
         * The merged provider filters by prefix; the filter policies of [providers] are not
         * combined. A provider that fails is logged and contributes no entries without affecting
         * the others; cancellation propagates.
         */
        fun merge(vararg providers: ArgumentSuggestions): ArgumentSuggestions {
            val copy = ObjectImmutableList(providers)
            return ArgumentSuggestions { info ->
                coroutineScope {
                    copy.mapTo(ObjectArrayList(copy.size)) { provider ->
                        async { suggestOrEmpty(provider, info) }
                    }
                        .awaitAll()
                        .flatten()
                }
            }
        }

        /**
         * Returns a provider that selects its entries from [branch] using the raw values of the
         * arguments before the current one.
         *
         * Yields no entries when the branch has no provider for the current position or rejects a
         * previous value, see [SuggestionsBranch.getNextSuggestion].
         */
        fun branch(branch: SuggestionsBranch): ArgumentSuggestions = ArgumentSuggestions { info ->
            val next = try {
                branch.getNextSuggestion(info.sender, *info.previousArgs.rawArguments().toTypedArray())
            } catch (_: CommandSyntaxException) {
                null
            }

            next?.suggest(info)?.let(::ObjectImmutableList).orEmpty()
        }

        private suspend fun suggestOrEmpty(
            provider: ArgumentSuggestions,
            info: SuggestionInfo,
        ): List<StringTooltip> = try {
            provider.suggest(info)
        } catch (failure: Throwable) {
            val ownTimeout = failure is TimeoutCancellationException &&
                    currentCoroutineContext().isActive
            if (failure is CancellationException && !ownTimeout) throw failure

            LOGGER.error("Suggestion provider failed and contributes no entries", failure)
            emptyList()
        }

        private fun withoutTooltip(value: String) = StringTooltip(value, null)
    }
}

private val LOGGER = ComponentLogger.logger()
