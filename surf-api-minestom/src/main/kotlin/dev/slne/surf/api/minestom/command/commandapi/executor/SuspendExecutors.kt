package dev.slne.surf.api.minestom.command.commandapi.executor

import dev.slne.surf.api.minestom.command.commandapi.exception.CommandSyntaxException
import dev.slne.surf.api.minestom.command.commandapi.exception.WrapperCommandSyntaxException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.command.CommandSender
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
val GENERIC_COMMAND_FAILURE: Component = Component.text(
    "Beim Ausführen des Befehls ist ein Fehler aufgetreten.",
    NamedTextColor.RED,
)

@ApiStatus.Internal
fun launchCommandExecutor(
    definition: ExecutorDefinition.Suspending,
    sender: CommandSender,
    arguments: CommandArguments,
    input: String,
): Job = definition.scopeProvider().launch {
    try {
        definition.executor(this, ExecutionInfo(sender, arguments, input))
    } catch (failure: Throwable) {
        handleCommandFailure(sender, input, failure)
    }
}

internal fun handleCommandFailure(
    sender: CommandSender,
    input: String,
    failure: Throwable,
    knownFailure: (Throwable) -> Component? = { null },
    unexpectedFailure: Component = GENERIC_COMMAND_FAILURE,
    syntaxFailurePolicy: SyntaxFailurePolicy = SyntaxFailurePolicy.RENDER,
) {
    if (failure is CancellationException) throw failure

    val knownMessage = knownFailure(failure)
    if (knownMessage != null) {
        sender.sendMessage(knownMessage)
        return
    }

    when {
        syntaxFailurePolicy == SyntaxFailurePolicy.USE_UNEXPECTED_FAILURE -> sendUnexpectedFailure(
            sender,
            input,
            failure,
            unexpectedFailure,
        )
        failure is WrapperCommandSyntaxException -> sendSyntaxFailure(sender, failure.exception)
        failure is CommandSyntaxException -> sendSyntaxFailure(sender, failure)
        else -> sendUnexpectedFailure(sender, input, failure, unexpectedFailure)
    }
}

@ApiStatus.Internal
fun handleCommandExecutorFailure(
    sender: CommandSender,
    input: String,
    failure: Throwable,
) = handleCommandFailure(sender, input, failure)

internal enum class SyntaxFailurePolicy {
    RENDER,
    USE_UNEXPECTED_FAILURE,
}

private fun sendUnexpectedFailure(
    sender: CommandSender,
    input: String,
    failure: Throwable,
    component: Component,
) {
    MinecraftServer.LOGGER.error("Failed to execute command '{}' for {}", input, sender, failure)
    sender.sendMessage(component)
}

private fun sendSyntaxFailure(sender: CommandSender, failure: CommandSyntaxException) {
    val component = failure.component ?: throw failure
    sender.sendMessage(component)
}
