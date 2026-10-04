package dev.slne.surf.api.minestom.server.impl.command.brigadier

import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.slne.surf.api.minestom.command.exception.componentOrNull
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.Component.translatable
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.translation.GlobalTranslator
import net.minestom.server.adventure.MinestomAdventure
import net.minestom.server.command.CommandSender
import net.minestom.server.entity.Player

private const val CONTEXT_LENGTH = 10
private const val HERE_KEY = "command.context.here"
private const val UNKNOWN_COMMAND_KEY = "command.unknown.command"
private const val UNKNOWN_ARGUMENT_KEY = "command.unknown.argument"

/**
 * Reports [failure] to [sender] in vanilla's two-line layout: the message, then the input up to the
 * offending position with the remainder underlined and marked.
 *
 * The heading distinguishes an input whose command label was never resolved from one that failed
 * partway through its arguments, which is the same distinction vanilla draws.
 */
internal fun reportSyntaxFailure(
    sender: CommandSender,
    input: String,
    failure: CommandSyntaxException
) {
    val raw = failure.rawMessage
    val message = raw?.componentOrNull()
        ?: raw?.string?.takeIf(String::isNotBlank)?.let { text ->
            text(text, NamedTextColor.RED)
        }

    report(sender, input, failure.cursor, message)
}

/**
 * Reports an input no registry could resolve, in the same layout vanilla uses for a command it does
 * not know: the heading, then the whole input marked as the offending part.
 */
internal fun reportUnknownCommand(sender: CommandSender, input: String) =
    report(sender, input, cursor = 0, message = null)

private fun report(
    sender: CommandSender,
    input: String,
    cursor: Int,
    message: Component?,
) {
    val heading = if (cursor > 0) UNKNOWN_ARGUMENT_KEY else UNKNOWN_COMMAND_KEY

    val body = text().append(translatable(heading, NamedTextColor.RED))
    if (message != null) {
        body.append(text(": ", NamedTextColor.RED)).append(message)
    }
    body.appendNewline().append(contextOf(input, cursor))

    sender.sendTranslated(body.build())
}

private fun contextOf(input: String, cursor: Int): Component {
    val clamped = cursor.coerceIn(0, input.length)
    val context = text()
        .color(NamedTextColor.GRAY)
        .clickEvent(ClickEvent.suggestCommand("/$input"))

    if (clamped > CONTEXT_LENGTH) context.append(text("..."))
    context.append(
        text(input.substring((clamped - CONTEXT_LENGTH).coerceAtLeast(0), clamped)),
    )
    if (clamped < input.length) {
        context.append(
            text(input.substring(clamped))
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.UNDERLINED),
        )
    }
    context.append(
        translatable(HERE_KEY)
            .color(NamedTextColor.RED)
            .decorate(TextDecoration.ITALIC),
    )

    return context.build()
}

/**
 * Sends [message] to this sender, resolving translation keys for senders that cannot resolve them
 * themselves. A player receives the keys untouched and renders them in its own language.
 */
private fun CommandSender.sendTranslated(message: Component) {
    if (this is Player) {
        sendMessage(message)
    } else {
        sendMessage(GlobalTranslator.render(message, MinestomAdventure.getDefaultLocale()))
    }
}
