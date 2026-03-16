package dev.slne.surf.microservice.runtime.microservice

import dev.slne.surf.microservice.api.microservice.Microservice
import dev.slne.surf.microservice.api.util.InternalMicroserviceApi
import dev.slne.surf.microservice.runtime.microservice.command.microserviceCommandManagerImpl
import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.core.api.util.requiredService
import dev.slne.surf.surfapi.standalone.SurfApiStandaloneBootstrap
import kotlinx.coroutines.*
import java.util.*

private val log = logger()

object MicroserviceLauncher {
    private val scope =
        CoroutineScope(Dispatchers.Default + CoroutineName("MicroserviceLauncher") + SupervisorJob() + CoroutineExceptionHandler { context, exception ->
            log.atSevere()
                .withCause(exception)
                .log("An uncaught exception occurred in the microservice scope on ${context[CoroutineName]}")
        })

    suspend fun launch(args: Array<String>) {
        microserviceCommandManagerImpl.initDefaultCommands()

        SurfApiStandaloneBootstrap.bootstrap()
        SurfApiStandaloneBootstrap.enable()

        microservice.bootstrap(args.toList())

        scope.launch(Dispatchers.IO + CoroutineName("MicroserviceCommandListener")) {
            val scanner = Scanner(System.`in`)

            while (isActive && scanner.hasNextLine()) {
                val line = scanner.nextLine()

                microserviceCommandManagerImpl.handleCommand(line, scope)
            }
        }

        log.atInfo().log("Microservice started successfully. Type 'exit' or 'quit' to shut down.")

        coroutineScope {
            launch {
                awaitCancellation()
            }.join()
        }
    }

    @InternalMicroserviceApi
    lateinit var microservice: Microservice
        internal set
}

suspend fun main(args: Array<String>) {
    MicroserviceLauncher.microservice = requiredService<Microservice>()
    Microservice.INSTANCE = MicroserviceLauncher.microservice

    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking {
            MicroserviceLauncher.microservice.disable()

            SurfApiStandaloneBootstrap.shutdown()
        }
    })

    MicroserviceLauncher.launch(args)
}