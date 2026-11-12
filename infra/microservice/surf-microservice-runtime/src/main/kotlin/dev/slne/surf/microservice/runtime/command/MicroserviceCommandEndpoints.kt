package dev.slne.surf.microservice.runtime.command

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.microservice.runtime.command.console.MicroserviceConsoleListener
import dev.slne.surf.microservice.runtime.command.transport.MicroserviceCommandServer
import dev.slne.surf.microservice.runtime.lifecycle.RuntimeComponent
import kotlinx.coroutines.*

/**
 * Accepts commands from the command socket and the console.
 *
 * [stop] stops the socket server and then the console listener, each of which cancels its running commands and waits
 * for them within its own timeout, so no command runs once [stop] returns unless it ignored cancellation for that
 * long. It does not wait for the console reader, which cannot be interrupted.
 */
internal class MicroserviceCommandEndpoints(
    private val server: MicroserviceCommandServer,
    private val console: MicroserviceConsoleListener,
) : RuntimeComponent {
    override val name = "command endpoints"

    private val log = logger()
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineName("MicroserviceCommandEndpoints") +
                CoroutineExceptionHandler { context, exception ->
                    log.atSevere()
                        .withCause(exception)
                        .log("Uncaught exception in %s", context[CoroutineName])
                },
    )

    override suspend fun start() {
        server.start(scope)
        console.start(scope)
    }

    override suspend fun stop() {
        try {
            server.stop()
        } finally {
            try {
                console.stop()
            } finally {
                scope.cancel("Microservice shutdown")
            }
        }
    }
}
