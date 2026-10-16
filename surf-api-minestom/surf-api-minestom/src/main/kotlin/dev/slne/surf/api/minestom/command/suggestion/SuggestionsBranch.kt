/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.surf.api.minestom.command.suggestion

import dev.slne.surf.api.minestom.command.exception.CommandSyntaxException
import dev.slne.surf.api.minestom.command.executor.CommandArguments
import dev.slne.surf.api.minestom.command.executor.ParsedArgument
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectImmutableList
import it.unimi.dsi.fastutil.objects.ObjectList
import net.kyori.adventure.text.Component
import net.minestom.server.command.CommandSender

/**
 * Suggestions for a sequence of arguments, selected by the values typed for the arguments before.
 *
 * [suggest] declares one provider per argument position, in order; `null` marks a position without
 * suggestions that accepts any value. [branch] adds alternative continuations that follow those
 * positions. A typed value at a position with a provider must equal one of that provider's
 * suggestions.
 *
 * [ArgumentSuggestions.branch] selects the provider by the number and values of the preceding
 * arguments, so the same branch is attached to every argument it covers.
 */
class SuggestionsBranch private constructor(
    suggestions: List<ArgumentSuggestions?>,
) {
    private val suggestions = ObjectImmutableList(suggestions)
    private val branches = ObjectArrayList<SuggestionsBranch>()

    /**
     * Adds [branches] as alternative continuations after this branch's own positions.
     */
    fun branch(vararg branches: SuggestionsBranch): SuggestionsBranch = apply {
        this.branches += branches
    }

    /**
     * Returns the provider for the argument that follows the values in [previousRaw], or `null`
     * when the branch defines no suggestions there.
     *
     * When several continuations match, their providers are merged with
     * [ArgumentSuggestions.merge].
     *
     * @throws CommandSyntaxException when a value in [previousRaw] is not among the suggestions of
     * its position, or when matching continuations disagree on whether a provider exists
     */
    suspend fun getNextSuggestion(
        sender: CommandSender,
        vararg previousRaw: String,
    ): ArgumentSuggestions? = traverse(
        mode = TraversalMode.NEXT,
        sender = sender,
        rawArguments = ObjectList.of(*previousRaw),
        processed = emptyList(),
    ).provider

    /**
     * Verifies that [previousRaw] is a complete, valid sequence for this branch.
     *
     * @throws CommandSyntaxException when a value is not among the suggestions of its position, or
     * when values are missing for a position whose provider has suggestions
     */
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

    /**
     * Matches [rawArguments] against the positions of this branch and then its continuations.
     *
     * [TraversalMode.NEXT] returns the provider for the first position without a value.
     * [TraversalMode.ENFORCE] only validates and always returns an empty result.
     */
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
        /**
         * Creates a branch with one provider per argument position, in order.
         *
         * A `null` entry marks a position without suggestions that accepts any value.
         */
        fun suggest(vararg suggestions: ArgumentSuggestions?): SuggestionsBranch =
            SuggestionsBranch(ObjectList.of(*suggestions))
    }
}
