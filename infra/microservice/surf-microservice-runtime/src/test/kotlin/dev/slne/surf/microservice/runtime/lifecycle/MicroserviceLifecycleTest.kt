package dev.slne.surf.microservice.runtime.lifecycle

import dev.slne.surf.microservice.api.Microservice
import dev.slne.surf.microservice.api.integration.MicroserviceIntegration
import dev.slne.surf.microservice.api.integration.MicroserviceIntegrationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MicroserviceLifecycleTest {
    private val events = CopyOnWriteArrayList<String>()

    @Test
    fun `starts and stops everything in order`() = runTest {
        val lifecycle = lifecycle()

        lifecycle.start(listOf("--flag"))
        lifecycle.shutdown()

        assertEquals(FULL_START + FULL_STOP, events)
    }

    @Test
    fun `passes the arguments to onBootstrap and installs the microservice`() = runTest {
        val microservice = RecordingMicroservice()
        val lifecycle = lifecycle(microservice = microservice)

        lifecycle.start(listOf("a", "b"))

        assertEquals(listOf("a", "b"), microservice.bootstrapArgs)
        assertSame(microservice, Microservice.instance)
    }

    @Test
    fun `gives integrations the validated name and the data path`() = runTest {
        val integration = RecordingIntegration("database", order = 1)
        lifecycle(integrations = listOf(integration)).start(emptyList())

        val context = integration.startContext!!
        assertEquals(SERVICE_NAME, context.name)
        assertEquals(DATA_PATH, context.dataPath)
    }

    @Test
    fun `orders integrations by order and then by name`() = runTest {
        val integrations = listOf(
            RecordingIntegration("zeta", order = 5),
            RecordingIntegration("beta", order = 10),
            RecordingIntegration("alpha", order = 10),
            RecordingIntegration("first", order = -1),
        )
        val lifecycle = lifecycle(integrations = integrations)

        lifecycle.start(emptyList())
        lifecycle.shutdown()

        val started = events.filter { it.endsWith(":start") && it.startsWith("integration") }
        val stopped = events.filter { it.endsWith(":stop") && it.startsWith("integration") }
        assertEquals(
            listOf("first", "zeta", "alpha", "beta").map { "integration:$it:start" },
            started,
        )
        assertEquals(listOf("beta", "alpha", "zeta", "first").map { "integration:$it:stop" }, stopped)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("startFailures")
    fun `stops only what started when the start fails`(failingStep: String, expectedEvents: List<String>) = runTest {
        val lifecycle = lifecycle(failingStep = failingStep)

        assertFailsWith<Exception> { lifecycle.start(emptyList()) }
        lifecycle.shutdown()

        assertEquals(expectedEvents, events)
    }

    @Test
    fun `rejects an invalid service name before starting surf-api`() = runTest {
        val lifecycle = lifecycle(microservice = RecordingMicroservice(name = "Invalid_Name"))

        val exception = assertFailsWith<IllegalStateException> { lifecycle.start(emptyList()) }
        lifecycle.shutdown()

        assertTrue("Invalid_Name" in exception.message.orEmpty())
        assertEquals(listOf("builtins", "create", "logging"), events)
    }

    @Test
    fun `continues the shutdown when stop steps fail`() = runTest {
        val lifecycle = lifecycle(failingStep = ALL_STOP_STEPS)

        lifecycle.start(emptyList())
        lifecycle.shutdown()

        assertEquals(FULL_START + FULL_STOP, events)
    }

    @Test
    fun `continues the shutdown when a stop step throws an error`() = runTest {
        val lifecycle = lifecycle(microservice = RecordingMicroservice(disableError = NotImplementedError()))

        lifecycle.start(emptyList())
        lifecycle.shutdown()

        assertEquals(FULL_START + FULL_STOP, events)
    }

    @Test
    fun `runs the shutdown only once`() = runTest {
        val lifecycle = lifecycle()
        lifecycle.start(emptyList())

        lifecycle.shutdown()
        lifecycle.shutdown()

        assertEquals(FULL_START + FULL_STOP, events)
    }

    @Test
    fun `a concurrent shutdown waits for the running one`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val lifecycle = lifecycle(endpoints = RecordingComponent("endpoints", stopGate = gate))
        lifecycle.start(emptyList())

        val first = launch { lifecycle.shutdown() }
        runCurrent()
        val second = async { lifecycle.shutdown() }
        runCurrent()
        assertFalse(second.isCompleted, "the second shutdown returned before the first completed")

        gate.complete(Unit)
        first.join()
        second.await()
        assertEquals(FULL_START + FULL_STOP, events)
    }

    @Test
    fun `a shutdown during the start waits for the start to finish`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val lifecycle = lifecycle(microservice = RecordingMicroservice(bootstrapGate = gate))

        val start = launch { lifecycle.start(emptyList()) }
        runCurrent()
        val shutdown = launch { lifecycle.shutdown() }
        runCurrent()
        assertFalse("surf-api:stop" in events, "the shutdown interleaved with the start")

        gate.complete(Unit)
        start.join()
        shutdown.join()
        assertEquals(FULL_START + FULL_STOP, events)
    }

    @Test
    fun `the shutdown releases awaitShutdownRequest`() = runTest {
        val lifecycle = lifecycle()
        lifecycle.start(emptyList())

        val waiter = async { lifecycle.awaitShutdownRequest() }
        runCurrent()
        assertFalse(waiter.isCompleted)

        lifecycle.shutdown()
        waiter.await()
    }

    @Test
    fun `starts only once`() = runTest {
        val lifecycle = lifecycle()
        lifecycle.start(emptyList())

        assertFailsWith<IllegalStateException> { lifecycle.start(emptyList()) }
    }

    @Test
    fun `refuses to start after the shutdown began`() = runTest {
        val lifecycle = lifecycle()
        lifecycle.shutdown()

        assertFailsWith<IllegalStateException> { lifecycle.start(emptyList()) }
        assertEquals(listOf("logging"), events)
    }

    private fun lifecycle(
        failingStep: String? = null,
        microservice: RecordingMicroservice = RecordingMicroservice(failingStep = failingStep),
        integrations: List<RecordingIntegration> = listOf(
            RecordingIntegration("nats", MicroserviceIntegration.MESSAGING_ORDER, failingStep),
            RecordingIntegration("database", MicroserviceIntegration.DATABASE_ORDER, failingStep),
        ),
        endpoints: RecordingComponent = RecordingComponent("endpoints", failingStep),
    ) = MicroserviceLifecycle(
        registerBuiltinCommands = { record("builtins", failingStep) },
        createMicroservice = {
            record("create", failingStep)
            microservice
        },
        surfApi = RecordingComponent("surf-api", failingStep),
        spark = RecordingComponent("spark", failingStep),
        loadIntegrations = { integrations },
        commandEndpoints = endpoints,
        shutdownLogging = { record("logging", failingStep) },
    )

    private fun record(event: String, failingStep: String?) {
        events += event
        if (event == failingStep || (failingStep == ALL_STOP_STEPS && event in FULL_STOP)) {
            throw IllegalStateException("$event failed")
        }
    }

    private inner class RecordingComponent(
        override val name: String,
        private val failingStep: String? = null,
        private val stopGate: CompletableDeferred<Unit>? = null,
    ) : RuntimeComponent {
        override suspend fun start() = record("$name:start", failingStep)

        override suspend fun stop() {
            stopGate?.await()
            record("$name:stop", failingStep)
        }
    }

    private inner class RecordingIntegration(
        override val name: String,
        override val order: Int,
        private val failingStep: String? = null,
    ) : MicroserviceIntegration {
        var startContext: MicroserviceIntegrationContext? = null

        override suspend fun start(context: MicroserviceIntegrationContext) {
            startContext = context
            record("integration:$name:start", failingStep)
        }

        override suspend fun stopAccepting(context: MicroserviceIntegrationContext) =
            record("integration:$name:stopAccepting", failingStep)

        override suspend fun stop(context: MicroserviceIntegrationContext) =
            record("integration:$name:stop", failingStep)
    }

    private inner class RecordingMicroservice(
        override val name: String = SERVICE_NAME,
        private val failingStep: String? = null,
        private val bootstrapGate: CompletableDeferred<Unit>? = null,
        private val disableError: Throwable? = null,
    ) : Microservice() {
        var bootstrapArgs: List<String>? = null

        override val dataPath: Path = DATA_PATH

        override suspend fun onBootstrap(args: List<String>) {
            bootstrapArgs = args
            bootstrapGate?.await()
            record("bootstrap", failingStep)
        }

        override suspend fun onDisable() {
            record("disable", failingStep)
            disableError?.let { throw it }
        }
    }

    companion object {
        private const val SERVICE_NAME = "test-service"
        private const val ALL_STOP_STEPS = "all stop steps"
        private val DATA_PATH = Path("build", "test-config")

        private val FULL_START = listOf(
            "builtins",
            "create",
            "surf-api:start",
            "spark:start",
            "integration:database:start",
            "integration:nats:start",
            "bootstrap",
            "endpoints:start",
        )

        private val FULL_STOP = listOf(
            "endpoints:stop",
            "integration:nats:stopAccepting",
            "integration:database:stopAccepting",
            "disable",
            "integration:nats:stop",
            "integration:database:stop",
            "spark:stop",
            "surf-api:stop",
            "logging",
        )

        @JvmStatic
        fun startFailures(): List<Arguments> = listOf(
            Arguments.of("builtins", listOf("builtins", "logging")),
            Arguments.of("create", listOf("builtins", "create", "logging")),
            Arguments.of("surf-api:start", listOf("builtins", "create", "surf-api:start", "logging")),
            Arguments.of(
                "spark:start",
                listOf("builtins", "create", "surf-api:start", "spark:start", "surf-api:stop", "logging"),
            ),
            Arguments.of(
                "integration:database:start",
                FULL_START.take(5) + listOf("spark:stop", "surf-api:stop", "logging"),
            ),
            Arguments.of(
                "integration:nats:start",
                FULL_START.take(6) + listOf("integration:database:stop", "spark:stop", "surf-api:stop", "logging"),
            ),
            Arguments.of(
                "bootstrap",
                FULL_START.take(7) + listOf(
                    "integration:nats:stop",
                    "integration:database:stop",
                    "spark:stop",
                    "surf-api:stop",
                    "logging",
                ),
            ),
            Arguments.of(
                "endpoints:start",
                FULL_START + listOf(
                    "integration:nats:stop",
                    "integration:database:stop",
                    "spark:stop",
                    "surf-api:stop",
                    "logging",
                ),
            ),
        )
    }
}
