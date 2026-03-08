package dev.slne.surf.microservice.api.command

import dev.slne.surf.microservice.api.command.commands.exitCommand
import dev.slne.surf.surfapi.core.api.util.freeze
import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.core.api.util.mutableObjectSetOf
import kotlinx.coroutines.CoroutineScope

object MicroserviceCommandManager {
    private val _commands = mutableObjectSetOf<MicroserviceCommand>()
    val commands get() = _commands.freeze()

    private val log = logger()
    private val SPLIT_REGEX = "\\s+".toRegex()

    fun initDefaultCommands() {
        exitCommand()
    }

    fun findCommand(label: String): MicroserviceCommand? {
        return _commands.firstOrNull {
            it.name.equals(
                label,
                true
            ) || it.aliases.any { alias -> alias.equals(label, true) }
        }
    }

    fun handleCommand(input: String, scope: CoroutineScope) {
        val parts = input.trim().split(SPLIT_REGEX)
        val label = parts.firstOrNull() ?: return

        val args = parts.drop(1)
        val command = findCommand(label) ?: run {
            printUnknownOrIncompleteCommand(label)
            return
        }

        command.execute(scope, args)
    }

    private fun printUnknownOrIncompleteCommand(label: String) {
        log.atWarning().log("Unknown or incomplete command: $label")
    }

    fun registerCommand(command: MicroserviceCommand) {
        _commands.add(command)

        log.atInfo()
            .log("Registered command ${command.name} with aliases ${command.aliases.joinToString(", ")}")
    }
}