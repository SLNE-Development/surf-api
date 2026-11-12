package dev.slne.surf.api.minestom.command.executor

import dev.slne.surf.api.minestom.command.exception.CommandSyntaxException
import dev.slne.surf.api.minestom.command.exception.WrapperCommandSyntaxException
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import kotlinx.coroutines.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.command.CommandSender

@InternalSurfApi
val GENERIC_COMMAND_FAILURE: Component = Component.text(
    "Beim Ausführen des Befehls ist ein Fehler aufgetreten.",
    NamedTextColor.RED,
)

/**
 * Launches the suspending executor of [definition] in its scope and reports a failure to [sender].
 *
 * A [TimeoutCancellationException] from the executor's own timeout is reported like any other
 * failure; cancellation of the launched coroutine propagates.
 */
@InternalSurfApi
fun launchCommandExecutor(
    definition: ExecutorDefinition.Suspending,
    sender: CommandSender,
    arguments: CommandArguments,
    input: String,
): Job = definition.scopeProvider().launch {
    try {
        definition.executor(this, ExecutionInfo(sender, arguments, input))
    } catch (failure: Throwable) {
        if (failure is TimeoutCancellationException && !isActive) throw failure
        handleCommandFailure(sender, input, failure)
    }
}

/**
 * Reports [failure] to [sender]: a syntax failure with its component, anything else as a logged
 * generic failure.
 *
 * Rethrows a [CancellationException] other than a [TimeoutCancellationException].
 */
internal fun handleCommandFailure(
    sender: CommandSender,
    input: String,
    failure: Throwable,
) {
    if (failure is CancellationException && failure !is TimeoutCancellationException) throw failure

    when (failure) {
        is WrapperCommandSyntaxException -> sendSyntaxFailure(sender, failure.exception)
        is CommandSyntaxException -> sendSyntaxFailure(sender, failure)
        else -> sendUnexpectedFailure(sender, input, failure, GENERIC_COMMAND_FAILURE)
    }
}

@InternalSurfApi
fun handleCommandExecutorFailure(
    sender: CommandSender,
    input: String,
    failure: Throwable,
) = handleCommandFailure(sender, input, failure)

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
