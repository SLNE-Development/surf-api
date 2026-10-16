package dev.slne.surf.api.minestom.server.impl.command

import com.mojang.brigadier.ParseResults
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import dev.slne.surf.api.minestom.command.exception.componentOrNull
import dev.slne.surf.api.minestom.event.EventRegistrar
import dev.slne.surf.api.minestom.extension.addListener
import net.minestom.server.MinecraftServer
import net.minestom.server.command.CommandSender
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerPacketEvent
import net.minestom.server.network.packet.client.play.ClientTabCompletePacket
import net.minestom.server.network.packet.server.play.TabCompletePacket
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * Answers tab completion for CommandAPI-owned commands from the Brigadier dispatcher.
 *
 * A request is handled only when its first word is exactly an owned label and a platform is
 * active; every other request is left to Minestom. A handled request is cancelled, and its
 * response is sent when the suggestion future completes, on whichever thread completes it. A
 * failed future is answered with an empty response. A response carries at most 1000 entries.
 */
internal class MinestomSuggestionListener(
    private val ownership: MinestomCommandOwnership,
) : EventRegistrar {

    override fun register(node: EventNode<Event>) {
        node.addListener(::handlePacket)
    }

    private fun handlePacket(event: PlayerPacketEvent) {
        val packet = event.packet as? ClientTabCompletePacket ?: return

        if (ownership.findInput(packet.text) == null) return
        val dispatcher = MinestomCommandAPIPlatform.activeDispatcher() ?: return

        event.isCancelled = true

        val command = StringReader(packet.text)
        if (command.canRead() && command.peek() == '/') {
            command.skip()
        }

        val suggestions = try {
            suggestionsFor(dispatcher.parse(command, event.player))
        } catch (failure: Throwable) {
            CompletableFuture.failedFuture(failure)
        }

        suggestions.whenComplete { result, failure ->
            val response = if (failure != null) {
                MinecraftServer.LOGGER.error(
                    "Failed to compute tab completion for '{}'",
                    packet.text,
                    failure,
                )
                TabCompletePacket(packet.transactionId, 0, 0, emptyList())
            } else {
                val limited = limitTo(result)
                TabCompletePacket(
                    packet.transactionId,
                    limited.range.start,
                    limited.range.length,
                    limited.list.map { suggestion ->
                        TabCompletePacket.Match(
                            suggestion.text,
                            suggestion.tooltip?.componentOrNull()
                        )
                    }
                )
            }

            event.player.sendPacket(response)
        }
    }

    companion object {
        private const val MAX_COMMAND_SUGGESTIONS = 1000

        /**
         * Collects the suggestions of the children of the node before the cursor that the sender
         * may use.
         *
         * Mirrors `CommandDispatcher.getCompletionSuggestions`, which lists every child without
         * checking its requirement. The returned future completes exceptionally when any child's
         * future does.
         */
        private fun suggestionsFor(
            parse: ParseResults<CommandSender>,
        ): CompletableFuture<Suggestions> {
            val context = parse.context
            val cursor = parse.reader.totalLength
            val suggestionContext = context.findSuggestionContext(cursor)
            val start = minOf(suggestionContext.startPos, cursor)

            val fullInput = parse.reader.string
            val truncatedInput = fullInput.substring(0, cursor)
            val truncatedInputLowerCase = truncatedInput.lowercase(Locale.ROOT)
            val source = context.source

            val futures = suggestionContext.parent.children
                .filter { child -> child.canUse(source) }
                .map { child ->
                    try {
                        child.listSuggestions(
                            context.build(truncatedInput),
                            SuggestionsBuilder(truncatedInput, truncatedInputLowerCase, start),
                        )
                    } catch (_: CommandSyntaxException) {
                        Suggestions.empty()
                    }
                }

            return CompletableFuture.allOf(*futures.toTypedArray()).thenApply {
                Suggestions.merge(fullInput, futures.map { future -> future.join() })
            }
        }

        private fun limitTo(
            suggestions: Suggestions,
            size: Int = MAX_COMMAND_SUGGESTIONS
        ): Suggestions {
            return if (suggestions.list.size <= size) {
                suggestions
            } else {
                Suggestions(suggestions.range, suggestions.list.subList(0, size))
            }
        }
    }
}
