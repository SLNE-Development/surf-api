package dev.slne.surf.microservice.runtime.microservice

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.standalone.SurfApiStandaloneBootstrap
import dev.slne.surf.microservice.api.common.util.InternalMicroserviceApi
import dev.slne.surf.microservice.api.microservice.Microservice
import dev.slne.surf.microservice.api.microservice.command.MicroserviceCommandContext
import dev.slne.surf.microservice.api.microservice.command.MicroserviceCommandOutput
import dev.slne.surf.microservice.api.microservice.command.MicroserviceCommandResult
import dev.slne.surf.microservice.api.microservice.command.MicroserviceCommandSource
import dev.slne.surf.microservice.runtime.microservice.command.microserviceCommandManagerImpl
import dev.slne.surf.microservice.runtime.microservice.command.transport.MicroserviceCommandClient
import dev.slne.surf.microservice.runtime.microservice.command.transport.MicroserviceCommandServer
import dev.slne.surf.microservice.runtime.microservice.spark.MicroserviceSpark
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

private val log = logger()

object MicroserviceLauncher {
    private val scope = CoroutineScope(
        Dispatchers.Default +
                CoroutineName("MicroserviceLauncher") +
                SupervisorJob() +
                CoroutineExceptionHandler { context, exception ->
                    log.atSevere()
                        .withCause(exception)
                        .log(
                            "An uncaught exception occurred in the microservice scope on " +
                                    context[CoroutineName]
                        )
                }
    )

    private val shutdownSignal = CompletableDeferred<Unit>()
    private val shutdownStarted = AtomicBoolean(false)

    private var commandServer: MicroserviceCommandServer? = null

    private val consoleOutput = object : MicroserviceCommandOutput {
        override fun sendLine(message: String) {
            log.atInfo().log(message)
        }

        override fun sendError(message: String) {
            log.atWarning().log(message)
        }
    }

    suspend fun launch(args: Array<String>) {
        microserviceCommandManagerImpl.initDefaultCommands()

        SurfApiStandaloneBootstrap.bootstrap()
        SurfApiStandaloneBootstrap.enable()

        microservice.bootstrap(args.toList())

        commandServer = MicroserviceCommandServer(
            scope = scope,
            commandManager = microserviceCommandManagerImpl,
            requestShutdown = ::requestShutdown
        ).also { server ->
            server.start()
        }

        startConsoleListener()

        log.atInfo()
            .log("Microservice started successfully. Type 'exit' or 'quit' to shut down.")

        shutdownSignal.await()
    }

    fun requestShutdown() {
        shutdownSignal.complete(Unit)
    }

    suspend fun shutdown() {
        if (!shutdownStarted.compareAndSet(false, true)) {
            return
        }

        withContext(NonCancellable) {
            try {
                commandServer?.stop()
            } catch (exception: Throwable) {
                log.atSevere()
                    .withCause(exception)
                    .log("Failed to stop the microservice command server.")
            }

            scope.cancel("Microservice shutdown")

            try {
                microservice.disable()
            } catch (exception: Throwable) {
                log.atSevere()
                    .withCause(exception)
                    .log("Failed to disable the microservice.")
            }

            try {
                MicroserviceSpark.onDisable()
            } catch (exception: Throwable) {
                log.atSevere()
                    .withCause(exception)
                    .log("Failed to disable the spark platform.")
            }

            try {
                SurfApiStandaloneBootstrap.shutdown()
            } catch (exception: Throwable) {
                log.atSevere()
                    .withCause(exception)
                    .log("Failed to shut down SurfApiStandaloneBootstrap.")
            }
        }
    }

    private fun startConsoleListener() {
        scope.launch(Dispatchers.IO + CoroutineName("MicroserviceCommandListener")) {
            val scanner = Scanner(System.`in`)

            while (isActive && scanner.hasNextLine()) {
                val input = scanner.nextLine()

                if (input.isBlank()) {
                    continue
                }

                launch(Dispatchers.Default + CoroutineName("MicroserviceConsoleCommand")) {
                    executeConsoleCommand(input)
                }
            }
        }
    }

    private suspend fun executeConsoleCommand(input: String) {
        val shutdownRequested = AtomicBoolean(false)

        val context = MicroserviceCommandContext(
            coroutineContext = currentCoroutineContext(),
            source = MicroserviceCommandSource.CONSOLE,
            output = consoleOutput,
            shutdownRequest = {
                shutdownRequested.set(true)
            }
        )

        when (
            val result = microserviceCommandManagerImpl.executeCommand(
                input,
                context
            )
        ) {
            MicroserviceCommandResult.Success -> Unit

            is MicroserviceCommandResult.Failure -> {
                consoleOutput.sendError(result.message)
            }
        }

        if (shutdownRequested.get()) {
            requestShutdown()
        }
    }

    @InternalMicroserviceApi
    lateinit var microservice: Microservice
        internal set
}

suspend fun main(args: Array<String>) {
    if (MicroserviceCommandClient.isClientInvocation(args)) {
        val exitCode = MicroserviceCommandClient.execute(
            args.drop(1)
        )

        exitProcess(exitCode)
    }


    MicroserviceLauncher.microservice = requiredService<Microservice>()
    Microservice.INSTANCE = MicroserviceLauncher.microservice

    val shutdownHook = Thread(
        {
            runBlocking {
                MicroserviceLauncher.shutdown()
            }
        },
        "MicroserviceShutdown"
    )

    Runtime.getRuntime().addShutdownHook(shutdownHook)


    try {
        MicroserviceSpark.onLoad()
        MicroserviceLauncher.launch(args)
    } finally {
        MicroserviceLauncher.shutdown()

        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook)
        } catch (_: IllegalStateException) {
        }
    }
}