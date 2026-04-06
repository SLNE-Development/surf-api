package dev.slne.surf.api.core.command.builder

import dev.slne.surf.api.core.messages.Colors
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import kotlin.math.max
import kotlin.math.min

/**
 * A builder for creating command exception messages with contextual information.
 * This builder constructs user-friendly error messages for commands, optionally including
 * a specified prefix, detailed error messages, and contextual information about the command input.
 *
 * @constructor Creates an instance of CommandExceptionBuilder with the specified parameters.
 * @param detailErrorMessage An optional detailed error message to include in the output.
 * @param input The command input string being processed.
 * @param cursor The position within the input where the error occurred.
 */
open class CommandExceptionBuilder(
    private val detailErrorMessage: String?,
    private val input: String?,
    private val cursor: Int
) {
    /**
     * Builds the command exception message with the default prefix ([Colors.PREFIX]).
     *
     * @return The built message
     */
    open fun build(): Component = build(Colors.PREFIX)

    /**
     * Builds the command exception message with the given prefix.
     *
     * @param prefix The prefix to add to the message
     * @return The built message
     */
    open fun build(prefix: Component?): Component {
        val builder = Component.text()
        val context = this.context

        if (prefix != null) {
            builder.append(prefix)
        }

        if (detailErrorMessage != null) {
            builder.append(Component.text(detailErrorMessage, Colors.WARNING))

            builder.appendNewline()
            if (prefix != null) {
                builder.append(prefix)
            }
        }

        if (context != null) {
            builder.append(Component.text("At position $cursor: ", Colors.ERROR))
            builder.append(context)
        }

        return builder.build()
    }

    protected open val context: Component?
        get() {
            if (input == null || cursor < 0) {
                return null
            }

            val builder = Component.text()
            val cursor = min(input.length, this.cursor)
            val start = max(0, (cursor - CONTEXT_AMOUNT))

            if (cursor > CONTEXT_AMOUNT) {
                builder.append(Component.text("...", Colors.ERROR))
            }

            for (i in start..<cursor) {
                builder.append(Component.text(input[i], Colors.ERROR, TextDecoration.UNDERLINED))
            }

            builder.append(Component.translatable("command.context.here", Colors.ERROR))

            return builder.build()
        }

    companion object {
        const val CONTEXT_AMOUNT: Int = 10
    }
}
