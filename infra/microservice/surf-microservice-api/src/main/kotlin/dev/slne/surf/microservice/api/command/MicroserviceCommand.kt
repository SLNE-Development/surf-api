package dev.slne.surf.microservice.api.command

/**
 * A console command of a microservice.
 *
 * Labels match [name] and [aliases] case-insensitively. Register commands through [MicroserviceCommands].
 */
abstract class MicroserviceCommand(
    val name: String,
    vararg val aliases: String,
) {
    init {
        (listOf(name) + aliases).forEach { label ->
            require(label.isNotBlank() && label.none(Char::isWhitespace)) {
                "Command label '$label' must not be blank or contain whitespace"
            }
        }
    }

    /**
     * One-line description shown by the `help` command.
     */
    open val description: String?
        get() = null

    /**
     * Runs the command with the arguments that followed the label.
     *
     * Throw [MicroserviceCommandException], usually through [MicroserviceCommandContext.fail], to report a failure
     * without a stack trace.
     */
    @Throws(MicroserviceCommandException::class)
    abstract suspend fun MicroserviceCommandContext.execute(args: List<String>)

    /**
     * Returns whether [label] selects this command.
     */
    fun matches(label: String): Boolean {
        return name.equals(label, ignoreCase = true)
                || aliases.any { alias -> alias.equals(label, ignoreCase = true) }
    }
}
