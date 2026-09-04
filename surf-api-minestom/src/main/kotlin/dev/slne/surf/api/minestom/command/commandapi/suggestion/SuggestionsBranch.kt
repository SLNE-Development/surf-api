/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.surf.api.minestom.command.commandapi.suggestion

import dev.slne.surf.api.minestom.command.commandapi.exception.CommandSyntaxException
import dev.slne.surf.api.minestom.command.commandapi.executor.CommandArguments
import dev.slne.surf.api.minestom.command.commandapi.executor.ParsedArgument
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectImmutableList
import it.unimi.dsi.fastutil.objects.ObjectList
import net.kyori.adventure.text.Component
import net.minestom.server.command.CommandSender

class SuggestionsBranch private constructor(
    suggestions: List<ArgumentSuggestions?>,
) {
    private val suggestions = ObjectImmutableList(suggestions)
    private val branches = ObjectArrayList<SuggestionsBranch>()

    fun branch(vararg branches: SuggestionsBranch): SuggestionsBranch = apply {
        this.branches += branches
    }

    suspend fun getNextSuggestion(
        sender: CommandSender,
        vararg previousRaw: String,
    ): ArgumentSuggestions? = traverse(
        mode = TraversalMode.NEXT,
        sender = sender,
        rawArguments = ObjectList.of(*previousRaw),
        processed = emptyList(),
    ).provider

    suspend fun enforceReplacements(
        sender: CommandSender,
        vararg previousRaw: String,
    ) {
        traverse(
            mode = TraversalMode.ENFORCE,
            sender = sender,
            rawArguments = ObjectList.of(*previousRaw),
            processed = emptyList(),
        )
    }

    private suspend fun traverse(
        mode: TraversalMode,
        sender: CommandSender,
        rawArguments: List<String>,
        processed: List<String>,
    ): TraversalResult {
        if (suggestions.isEmpty && branches.isEmpty) {
            return TraversalResult(null)
        }

        val currentProcessed = ObjectArrayList(processed)
        for (provider in suggestions) {
            if (currentProcessed.size == rawArguments.size) {
                if (mode == TraversalMode.NEXT) {
                    return TraversalResult(provider)
                }

                if (provider == null) {
                    return TraversalResult(null)
                }

                val missingInfo = suggestionInfo(sender, currentProcessed, "")
                if (provider.suggest(missingInfo).isNotEmpty()) {
                    throw syntax("Expected more arguments", rawArguments, currentProcessed)
                }

                return TraversalResult(null)
            }

            val candidate = rawArguments[currentProcessed.size]
            if (provider != null) {
                val info = suggestionInfo(sender, currentProcessed, "")
                if (provider.suggest(info).none { it.suggestion == candidate }) {
                    val message =
                        if (currentProcessed.isEmpty) "Unknown command" else "Unknown argument"
                    throw syntax(message, rawArguments, currentProcessed)
                }
            }

            currentProcessed += candidate
        }

        if (branches.isEmpty) {
            return TraversalResult(null)
        }

        val successes = ObjectArrayList<TraversalResult>()
        val failures = ObjectArrayList<CommandSyntaxException>()

        for (branch in branches) {
            try {
                successes += branch.traverse(mode, sender, rawArguments, currentProcessed)
            } catch (exception: CommandSyntaxException) {
                failures += exception
            }
        }

        if (successes.isEmpty) {
            throw failures.maxByOrNull { it.cursor ?: -1 }
                ?: syntax("Unknown argument", rawArguments, currentProcessed)
        }

        if (mode == TraversalMode.ENFORCE) {
            return TraversalResult(null)
        }

        if (successes.size == 1) {
            return successes.single()
        }

        val providers = successes.mapTo(ObjectArrayList(successes.size), TraversalResult::provider)

        if (providers.all { it == null }) {
            return TraversalResult(null)
        }

        if (providers.any { it == null }) {
            throw syntax(
                "Multiple paths for a SuggestionsBranch must either be all null or all non-null",
                rawArguments,
                currentProcessed,
            )
        }

        return TraversalResult(ArgumentSuggestions.merge(*providers.filterNotNull().toTypedArray()))
    }

    private fun suggestionInfo(
        sender: CommandSender,
        processed: List<String>,
        currentArgument: String,
    ): SuggestionInfo {
        val parsed = processed.mapIndexedTo(ObjectArrayList(processed.size)) { index, raw ->
            ParsedArgument(index.toString(), raw, raw, present = true)
        }

        return SuggestionInfo(
            sender = sender,
            previousArgs = CommandArguments.of(parsed),
            currentInput = processed.joinToString(
                " ",
                postfix = if (processed.isEmpty()) "" else " "
            ),
            currentArg = currentArgument,
        )
    }

    private fun syntax(
        message: String,
        rawArguments: List<String>,
        processed: List<String>,
    ) = CommandSyntaxException(
        component = Component.text(message),
        input = rawArguments.joinToString(" "),
        cursor = processed.joinToString(" ").length + if (processed.isEmpty()) 0 else 1,
    )

    private data class TraversalResult(val provider: ArgumentSuggestions?)

    private enum class TraversalMode {
        NEXT,
        ENFORCE,
    }

    companion object {
        fun suggest(vararg suggestions: ArgumentSuggestions?): SuggestionsBranch =
            SuggestionsBranch(ObjectList.of(*suggestions))
    }
}
