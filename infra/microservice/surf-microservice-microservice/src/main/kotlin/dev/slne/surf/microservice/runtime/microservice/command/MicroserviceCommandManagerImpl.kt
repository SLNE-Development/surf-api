package dev.slne.surf.microservice.runtime.microservice.command

import com.google.auto.service.AutoService
import dev.slne.surf.microservice.api.microservice.command.MicroserviceCommand
import dev.slne.surf.microservice.api.microservice.command.MicroserviceCommandManager
import dev.slne.surf.microservice.runtime.microservice.command.commands.exitCommand
import dev.slne.surf.microservice.runtime.microservice.command.commands.sparkCommand
import dev.slne.surf.surfapi.core.api.util.freeze
import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@AutoService(MicroserviceCommandManager::class)
class MicroserviceCommandManagerImpl : MicroserviceCommandManager {
    private val _commands = mutableObjectListOf<MicroserviceCommand>()
    override val commands get() = _commands.freeze()

    private val log = logger()
    private val splitRegex = "\\s+".toRegex()

    fun initDefaultCommands() {
        exitCommand()
        sparkCommand()
    }

    override fun findCommand(label: String): MicroserviceCommand? {
        return _commands.firstOrNull {
            it.name.equals(
                label,
                true
            ) || it.aliases.any { alias -> alias.equals(label, true) }
        }
    }

    override fun handleCommand(input: String, scope: CoroutineScope) {
        val parts = input.trim().split(splitRegex)
        val label = parts.firstOrNull() ?: return

        val args = parts.drop(1)
        val command = findCommand(label) ?: run {
            printUnknownOrIncompleteCommand(label)
            return
        }

        scope.launch {
            with(command) { execute(args) }
        }
    }

    private fun printUnknownOrIncompleteCommand(label: String) {
        log.atWarning().log("Unknown or incomplete command: $label")
    }

    override fun registerCommand(command: MicroserviceCommand) {
        _commands.add(command)

        log.atInfo()
            .log("Registered command ${command.name} with aliases ${command.aliases.joinToString(", ")}")
    }
}

val microserviceCommandManagerImpl =
    MicroserviceCommandManager.INSTANCE as MicroserviceCommandManagerImpl