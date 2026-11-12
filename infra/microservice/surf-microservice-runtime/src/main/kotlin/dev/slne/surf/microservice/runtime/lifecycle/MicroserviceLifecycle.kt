package dev.slne.surf.microservice.runtime.lifecycle

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.microservice.api.Microservice
import dev.slne.surf.microservice.api.integration.MicroserviceIntegration
import dev.slne.surf.microservice.api.integration.MicroserviceIntegrationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Starts and stops a microservice and everything it depends on in a fixed order.
 *
 * Start order: built-in commands, microservice instance (installed as [Microservice.instance]), name validation,
 * [surfApi], [spark], integrations ascending by `order` and then by name, [Microservice.onBootstrap],
 * [commandEndpoints].
 *
 * Stop order: [commandEndpoints], `stopAccepting` of the integrations descending, [Microservice.onDisable], `stop`
 * of the integrations descending, [spark], [surfApi], [shutdownLogging]. The same order applies after a failed
 * [start], limited to what started: a step whose start threw counts as not started, and [Microservice.onDisable] runs
 * only if [Microservice.onBootstrap] returned. A failing stop step is logged and the shutdown continues.
 *
 * [start] and [shutdown] are serialized, so a shutdown requested during [start] waits for it to return or fail.
 * [shutdown] runs once and is not cancellable. Later and concurrent calls wait until the first one completes.
 */
internal class MicroserviceLifecycle(
    private val registerBuiltinCommands: () -> Unit,
    private val createMicroservice: () -> Microservice,
    private val surfApi: RuntimeComponent,
    private val spark: RuntimeComponent,
    private val loadIntegrations: () -> List<MicroserviceIntegration>,
    private val commandEndpoints: RuntimeComponent,
    private val shutdownLogging: () -> Unit,
) {
    private val log = logger()
    private val transitionLock = Mutex()
    private val shutdownRequest = CompletableDeferred<Unit>()
    private val shutdownCompletion = CompletableDeferred<Unit>()
    private val shutdownStarted = AtomicBoolean()

    // Guarded by transitionLock
    private var startAttempted = false
    private var bootstrapped = false
    private var commandEndpointsStarted = false
    private var microservice: Microservice? = null
    private var integrationContext: RuntimeIntegrationContext? = null
    private val startedComponents = ArrayList<RuntimeComponent>()
    private val startedIntegrations = ArrayList<MicroserviceIntegration>()

    /**
     * Starts everything in order and returns once the microservice runs.
     *
     * The caller must call [shutdown] after a failure to stop what already started.
     *
     * @throws IllegalStateException if called more than once or after [shutdown] began
     */
    suspend fun start(args: List<String>) = transitionLock.withLock {
        check(!startAttempted) { "The microservice lifecycle can only be started once." }
        check(!shutdownStarted.get()) { "The microservice is already shutting down." }
        startAttempted = true

        registerBuiltinCommands()
        val microservice = createMicroservice().also(::install)
        val context = RuntimeIntegrationContext(
            microservice = microservice,
            name = MicroserviceNames.requireValid(microservice.name),
            dataPath = microservice.dataPath,
        )
        integrationContext = context

        startComponent(surfApi)
        startComponent(spark)
        startIntegrations(context)
        microservice.onBootstrap(args)
        bootstrapped = true
        commandEndpoints.start()
        commandEndpointsStarted = true

        log.atInfo()
            .log("Microservice %s started. Type 'help' for a list of commands.", context.name)
    }

    /**
     * Lets [awaitShutdownRequest] return. The shutdown itself happens in [shutdown].
     */
    fun requestShutdown() {
        shutdownRequest.complete(Unit)
    }

    /**
     * Suspends until [requestShutdown] or [shutdown] was called, which may have happened before this call.
     */
    suspend fun awaitShutdownRequest() {
        shutdownRequest.await()
    }

    /**
     * Stops everything that started in the order described for the class and completes the shutdown request.
     *
     * A failing stop step is logged and does not make this function throw.
     */
    suspend fun shutdown() {
        requestShutdown()
        if (!shutdownStarted.compareAndSet(false, true)) {
            shutdownCompletion.await()
            return
        }

        try {
            withContext(NonCancellable) {
                transitionLock.withLock {
                    stopStarted()
                }
            }
        } finally {
            shutdownCompletion.complete(Unit)
        }
    }

    private fun install(microservice: Microservice) {
        Microservice.install(microservice)
        this.microservice = microservice
    }

    private suspend fun startComponent(component: RuntimeComponent) {
        component.start()
        startedComponents.add(component)
        log.atInfo()
            .log("Started %s", component.name)
    }

    private suspend fun startIntegrations(context: RuntimeIntegrationContext) {
        val integrations = loadIntegrations().sortedWith(compareBy({ it.order }, { it.name }))
        for (integration in integrations) {
            integration.start(context)
            startedIntegrations += integration
            log.atInfo()
                .log("Started integration %s", integration.name)
        }
    }

    private suspend fun stopStarted() {
        if (commandEndpointsStarted) {
            runStopStep("stop the ${commandEndpoints.name}") {
                commandEndpoints.stop()
            }
        }

        forEachStartedIntegrationDescending("stop accepting work in") { context ->
            stopAccepting(context)
        }

        if (bootstrapped) {
            runStopStep("disable the microservice") {
                microservice?.onDisable()
            }
        }

        forEachStartedIntegrationDescending("stop") { context ->
            stop(context)
        }

        startedComponents.asReversed().forEach { component ->
            runStopStep("stop ${component.name}") {
                component.stop()
            }
        }

        runStopStep("shut down logging") {
            shutdownLogging()
        }
    }

    private suspend fun forEachStartedIntegrationDescending(
        action: String,
        block: suspend MicroserviceIntegration.(MicroserviceIntegrationContext) -> Unit,
    ) {
        val context = integrationContext ?: return
        startedIntegrations.asReversed().forEach { integration ->
            runStopStep("$action integration ${integration.name}") {
                integration.block(context)
            }
        }
    }

    private suspend fun runStopStep(description: String, step: suspend () -> Unit) {
        try {
            step()
        } catch (exception: Throwable) {
            log.atSevere()
                .withCause(exception)
                .log("Failed to %s", description)
        }
    }
}
