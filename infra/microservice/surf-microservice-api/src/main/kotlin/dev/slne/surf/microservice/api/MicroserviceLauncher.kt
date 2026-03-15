package dev.slne.surf.microservice.api

import dev.slne.surf.microservice.api.command.MicroserviceCommandManager
import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.core.api.util.requiredService
import kotlinx.coroutines.*
import java.util.*

private val log = logger()

suspend fun main(args: Array<String>) {
    Microservice.INSTANCE = requiredService<Microservice>()

    val scope =
        CoroutineScope(Dispatchers.Default + CoroutineName("MicroserviceLauncher") + SupervisorJob() + CoroutineExceptionHandler { context, exception ->
            log.atSevere()
                .withCause(exception)
                .log("An uncaught exception occurred in the microservice scope on ${context[CoroutineName]}")
        })

    MicroserviceCommandManager.initDefaultCommands()

    scope.launch {
        microservice.bootstrap(args.toList())
    }

    scope.launch(Dispatchers.IO + CoroutineName("MicroserviceCommandListener")) {
        val scanner = Scanner(System.`in`)
        while (isActive && scanner.hasNextLine()) {
            val line = scanner.nextLine()

            MicroserviceCommandManager.handleCommand(line, scope)
        }
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking {
            microservice.disable()
        }
    })

    log.atInfo().log("Microservice started successfully. Type 'exit' or 'quit' to shut down.")

    scope.launch {
        awaitCancellation()
    }.join()
}