package dev.slne.surf.microservice.runtime.command

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.microservice.api.command.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs command invocations against the commands that [findCommand] resolves.
 *
 * A [MicroserviceCommandException] becomes a [MicroserviceCommandResult.Failure] with its message; any other
 * exception or error, except a [VirtualMachineError], is logged with its stack trace and reported as a failure, so
 * the caller always receives a response. A [CancellationException] propagates only if
 * the calling coroutine was cancelled; one thrown by the command itself, such as a timeout, is a failure. An
 * invocation completes after all coroutines it started in its context.
 */
internal class MicroserviceCommandDispatcher(
    private val findCommand: (label: String) -> MicroserviceCommand? = MicroserviceCommands::find,
) {
    private val log = logger()

    /**
     * Tokenizes [line] with [CommandLineTokenizer] and runs the command it names.
     */
    suspend fun dispatch(
        line: String,
        source: MicroserviceCommandSource,
        output: MicroserviceCommandOutput,
    ): CommandDispatch {
        val tokens = try {
            CommandLineTokenizer.tokenize(line)
        } catch (exception: CommandLineSyntaxException) {
            return failed(exception.message.orEmpty())
        }

        if (tokens.isEmpty()) {
            return failed("No command was specified. $HELP_HINT")
        }

        return dispatch(tokens.first(), tokens.drop(1), source, output)
    }

    suspend fun dispatch(
        label: String,
        args: List<String>,
        source: MicroserviceCommandSource,
        output: MicroserviceCommandOutput,
    ): CommandDispatch {
        val command = findCommand(label) ?: return failed("Unknown command '$label'. $HELP_HINT")
        val shutdownRequested = AtomicBoolean()

        val result = try {
            execute(command, args, source, output) { shutdownRequested.set(true) }
            MicroserviceCommandResult.Success
        } catch (exception: MicroserviceCommandException) {
            MicroserviceCommandResult.Failure(exception.message)
        } catch (error: VirtualMachineError) {
            throw error
        } catch (exception: Throwable) {
            if (exception is CancellationException) currentCoroutineContext().ensureActive()
            val message =
                "Command '${command.name}' failed: ${exception.message ?: exception.javaClass.name}"
            log.atSevere()
                .withCause(exception)
                .log("%s", message)
            MicroserviceCommandResult.Failure(message)
        }

        return CommandDispatch(result, shutdownRequested.get())
    }

    private suspend fun execute(
        command: MicroserviceCommand,
        args: List<String>,
        source: MicroserviceCommandSource,
        output: MicroserviceCommandOutput,
        requestShutdown: () -> Unit,
    ) = coroutineScope {
        val context = MicroserviceCommandContext(coroutineContext, source, output, requestShutdown)
        with(command) { context.execute(args) }
    }

    private fun failed(message: String): CommandDispatch {
        return CommandDispatch(MicroserviceCommandResult.Failure(message), false)
    }

    private companion object {
        const val HELP_HINT = "Type 'help' for a list of commands."
    }
}
