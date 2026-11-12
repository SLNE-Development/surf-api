package dev.slne.surf.microservice.api.command

/**
 * A console command of a microservice.
 *
 * A command runs when the first word of a line on the standard input of the process is one of its labels, or when
 * `microservicectl` names one. Labels match [name] and [aliases] case-insensitively. On the console, the rest of the
 * line becomes the arguments: whitespace separates them, single quotes keep their content literally, double quotes
 * group their content, and a backslash outside single quotes escapes the next character. `microservicectl` passes
 * its own arguments unchanged.
 *
 * Register a command with [MicroserviceCommands.register], or create and register one in a single call with
 * [microserviceCommand].
 *
 * ```kotlin
 * class BanCommand : MicroserviceCommand("ban", "b") {
 *     override val description = "Bans a player: ban <player> [reason]"
 *
 *     override suspend fun MicroserviceCommandContext.execute(args: List<String>) {
 *         val player = args.firstOrNull() ?: fail("Usage: ban <player> [reason]")
 *         val reason = args.drop(1).joinToString(" ").ifEmpty { "No reason" }
 *         sendLine("Banned $player: $reason")
 *     }
 * }
 *
 * MicroserviceCommands.register(BanCommand())
 * ```
 *
 * @property name the primary label, listed by the `help` command
 * @property aliases further labels that select this command
 * @throws IllegalArgumentException if [name] or one of [aliases] is blank or contains whitespace
 */
public abstract class MicroserviceCommand(
    public val name: String,
    public vararg val aliases: String,
) {
    init {
        (listOf(name) + aliases).forEach { label ->
            require(label.isNotBlank() && label.none(Char::isWhitespace)) {
                "Command label '$label' must not be blank or contain whitespace"
            }
        }
    }

    /**
     * Gets the one-line description that the `help` command shows, or `null` for none.
     */
    public open val description: String?
        get() = null

    /**
     * Runs the command with the arguments that followed the label.
     *
     * Throw [MicroserviceCommandException], usually through [MicroserviceCommandContext.fail], to report a failure
     * without a stack trace. Any other exception is logged with its stack trace and reported to the caller as
     * `Command '<name>' failed: <message>`. The invocation is cancelled when the microservice shuts down.
     */
    @Throws(MicroserviceCommandException::class)
    public abstract suspend fun MicroserviceCommandContext.execute(args: List<String>)

    /**
     * Returns whether [label] equals [name] or one of [aliases], ignoring case.
     */
    public fun matches(label: String): Boolean {
        return name.equals(label, ignoreCase = true)
                || aliases.any { alias -> alias.equals(label, ignoreCase = true) }
    }
}
