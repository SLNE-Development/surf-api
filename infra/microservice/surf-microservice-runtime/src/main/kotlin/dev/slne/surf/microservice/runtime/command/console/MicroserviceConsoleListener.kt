package dev.slne.surf.microservice.runtime.command.console

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.microservice.api.command.MicroserviceCommandResult
import dev.slne.surf.microservice.api.command.MicroserviceCommandSource
import dev.slne.surf.microservice.runtime.command.LoggingCommandOutput
import dev.slne.surf.microservice.runtime.command.MicroserviceCommandDispatcher
import kotlinx.coroutines.*
import java.io.InputStream
import kotlin.time.Duration

/**
 * Runs every non-blank line read from [input] as a command and logs its output. A command that fails with an
 * uncaught throwable does not stop the listener.
 *
 * Reading blocks a thread of [Dispatchers.IO] that cancellation cannot interrupt; it ends with the end of [input]
 * or the process. Commands therefore run outside the reader coroutine, so [stop] can wait for them without waiting
 * for the reader.
 *
 * [start] and [stop] must not run concurrently. [stop] cancels the reader and all running commands and waits up to
 * [stopTimeout] for the commands to finish.
 */
internal class MicroserviceConsoleListener(
    private val dispatcher: MicroserviceCommandDispatcher,
    private val requestShutdown: () -> Unit,
    private val stopTimeout: Duration,
    private val input: InputStream = System.`in`,
) {
    private val log = logger()

    private var reader: Job? = null
    private var commands: CompletableJob? = null

    fun start(scope: CoroutineScope) {
        check(reader == null) { "The microservice console listener is already running." }

        val commandJob = SupervisorJob(scope.coroutineContext.job)
        commands = commandJob

        val commandScope = CoroutineScope(scope.coroutineContext + commandJob + Dispatchers.Default)

        reader = scope.launch(Dispatchers.IO + CoroutineName("MicroserviceConsoleListener")) {
            val lines = input.bufferedReader()
            while (isActive) {
                val line = lines.readLine() ?: break
                if (line.isBlank()) continue

                commandScope.launch(CoroutineName("MicroserviceConsoleCommand")) {
                    execute(line)
                }
            }
        }
    }

    suspend fun stop() {
        reader?.cancel()
        reader = null

        val commandJob = commands ?: return
        commands = null
        commandJob.cancel()
        withTimeoutOrNull(stopTimeout) {
            commandJob.join()
        } ?: log.atWarning()
            .log("Console commands did not finish within %s after cancellation.", stopTimeout)
    }

    private suspend fun execute(line: String) {
        val dispatch = dispatcher.dispatch(
            line,
            MicroserviceCommandSource.CONSOLE,
            LoggingCommandOutput,
        )

        val result = dispatch.result
        if (result is MicroserviceCommandResult.Failure) {
            LoggingCommandOutput.sendError(result.message)
        }

        if (dispatch.shutdownRequested) {
            requestShutdown()
        }
    }
}
