package dev.slne.surf.api.minestom.server.impl.command

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.suggestion.Suggestions
import dev.slne.surf.api.minestom.command.exception.componentOrNull
import dev.slne.surf.api.minestom.event.EventRegistrar
import dev.slne.surf.api.minestom.extension.addListener
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerPacketEvent
import net.minestom.server.network.packet.client.play.ClientTabCompletePacket
import net.minestom.server.network.packet.server.play.TabCompletePacket

/**
 * Answers tab completion for CommandAPI-owned commands from the Brigadier dispatcher.
 *
 * Input belonging to another registry is left to Minestom. Brigadier reports both the entries and
 * the range they replace, so the client receives the same replacement span it would get from vanilla.
 */
internal class MinestomSuggestionListener(
    private val ownership: MinestomCommandOwnership,
) : EventRegistrar {

    override fun register(node: EventNode<Event>) {
        node.addListener(::handlePacket)
    }

    private fun handlePacket(event: PlayerPacketEvent) {
        val packet = event.packet as? ClientTabCompletePacket ?: return
        val command = StringReader(packet.text)
        if (command.canRead() && command.peek() == '/') {
            command.skip()
        }

        val commandName = command.peekUnquotedString()

        if (ownership.find(commandName) == null) return

        event.isCancelled = true

        val dispatcher = MinestomCommandAPIPlatform.activeDispatcher() ?: return
        val parse = dispatcher.parse(command, event.player)

        dispatcher.getCompletionSuggestions(parse).thenAccept { suggestions ->
            val suggestions = limitTo(suggestions)
            event.player.sendPacket(
                TabCompletePacket(
                    packet.transactionId,
                    suggestions.range.start,
                    suggestions.range.length,
                    suggestions.list.map { suggestion ->
                        TabCompletePacket.Match(
                            suggestion.text,
                            suggestion.tooltip?.componentOrNull()
                        )
                    }
                )
            )
        }
    }

    companion object {
        private const val MAX_COMMAND_SUGGESTIONS = 1000

        private fun StringReader.peekUnquotedString(): String {
            val start = cursor
            val string = readUnquotedString()
            cursor = start
            return string
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
