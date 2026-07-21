package dev.slne.surf.microservice.runtime.microservice.command

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.messages.DefaultFontInfo
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.microservice.api.microservice.command.MicroserviceCommand
import dev.slne.surf.microservice.api.microservice.command.MicroserviceCommandContext
import dev.slne.surf.microservice.api.microservice.command.MicroserviceCommandManager
import dev.slne.surf.microservice.api.microservice.command.MicroserviceCommandManager.Companion.findCommand
import dev.slne.surf.microservice.api.microservice.command.MicroserviceCommandResult
import dev.slne.surf.microservice.runtime.microservice.command.commands.exitCommand
import dev.slne.surf.microservice.runtime.microservice.command.commands.sparkCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.cancellation.CancellationException

@AutoService(MicroserviceCommandManager::class)
class MicroserviceCommandManagerImpl : MicroserviceCommandManager {
    override val commands = CopyOnWriteArrayList<MicroserviceCommand>()

    private val log = logger()
    private val splitRegex = "\\s+".toRegex()

    fun initDefaultCommands() {
        exitCommand()
        sparkCommand()
    }

    override fun findCommand(label: String): MicroserviceCommand? {
        return commands.firstOrNull { command ->
            command.name.equals(label, ignoreCase = true) ||
                    command.aliases.any { alias -> alias.equals(label, ignoreCase = true) }
        }
    }

    override suspend fun executeCommand(
        input: String,
        context: MicroserviceCommandContext
    ): MicroserviceCommandResult {
        val normalizedInput = input.trim()

        if (normalizedInput.isEmpty()) {
            return MicroserviceCommandResult.Failure("No command was specified.")
        }

        val parts = normalizedInput.split(splitRegex)
        val label = parts.first()
        val args = parts.drop(1)

        return executeCommand(label, args, context)
    }

    override suspend fun executeCommand(
        label: String,
        args: List<String>,
        context: MicroserviceCommandContext
    ): MicroserviceCommandResult {
        val command = findCommand(label)

        if (command == null) {
            val message = "Unknown or incomplete command: $label"

            log.atWarning().log(message)

            return MicroserviceCommandResult.Failure(message)
        }

        return try {
            with(command) {
                context.execute(args)
            }

            MicroserviceCommandResult.Success
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            val reason = exception.message ?: exception.javaClass.simpleName
            val message = "Command '${command.name}' failed: $reason"

            log.atSevere()
                .withCause(exception)
                .log(message)

            MicroserviceCommandResult.Failure(message)
        }
    }

    override fun registerCommand(command: MicroserviceCommand) {
        require(findCommand(command.name) == null) {
            "A microservice command named '${command.name}' is already registered."
        }

        val duplicateAlias = command.aliases.firstOrNull { alias ->
            findCommand(alias) != null
        }

        require(duplicateAlias == null) {
            "The microservice command alias '$duplicateAlias' is already registered."
        }

        commands.add(command)

        log.atInfo().log(
            buildString {
                append("Registered command ")
                append(command.name)

                if (command.aliases.isNotEmpty()) {
                    append(" with aliases ")
                    append(command.aliases.joinToString(", "))
                }
            }
        )
    }
}

val microserviceCommandManagerImpl =
    MicroserviceCommandManager.INSTANCE as MicroserviceCommandManagerImpl