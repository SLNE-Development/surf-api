package dev.slne.surf.microservice.runtime.command.console

import dev.slne.surf.microservice.api.command.MicroserviceCommand
import dev.slne.surf.microservice.api.command.MicroserviceCommandContext
import dev.slne.surf.microservice.runtime.command.MicroserviceCommandDispatcher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class MicroserviceConsoleListenerTest {
    private val uncaught = CopyOnWriteArrayList<Throwable>()
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, exception -> uncaught += exception },
    )
    private val console = PipedOutputStream()
    private val input = PipedInputStream(console)

    private val executed = CopyOnWriteArrayList<String>()
    private val recorded = CompletableDeferred<Unit>()
    private val crashed = CompletableDeferred<Unit>()
    private val shutdownRequest = CompletableDeferred<Unit>()
    private val lingerStarted = CompletableDeferred<Unit>()
    private val lingerCleanedUp = AtomicBoolean()
    private val stuckStarted = CompletableDeferred<Unit>()
    private val stuckRelease = CompletableDeferred<Unit>()

    private val commands = listOf(
        command("record") { args ->
            executed += args.joinToString(" ")
            recorded.complete(Unit)
        },
        command("crash") {
            crashed.complete(Unit)
            throw InternalError("crashed")
        },
        command("exit") { requestShutdown() },
        command("linger") {
            lingerStarted.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) {
                    delay(100)
                    lingerCleanedUp.set(true)
                }
            }
        },
        command("stuck") {
            stuckStarted.complete(Unit)
            withContext(NonCancellable) { stuckRelease.await() }
        },
    )
    private val listener = MicroserviceConsoleListener(
        dispatcher = MicroserviceCommandDispatcher { label -> commands.firstOrNull { it.matches(label) } },
        requestShutdown = { shutdownRequest.complete(Unit) },
        stopTimeout = STOP_TIMEOUT,
        input = input,
    )

    @AfterEach
    fun tearDown() {
        stuckRelease.complete(Unit)
        console.close()
        scope.cancel()
    }

    @Test
    fun `runs non-blank lines and requests the shutdown after exit`() = runBlocking {
        listener.start(scope)

        type("record \"a b\" c\n   \nexit\n")

        withTimeout(TIMEOUT) {
            shutdownRequest.await()
            recorded.await()
        }
        assertEquals(listOf("a b c"), executed)
    }

    @Test
    fun `keeps reading after a command throws a virtual machine error`() = runBlocking {
        listener.start(scope)

        type("crash\n")
        withTimeout(TIMEOUT) { crashed.await() }
        delay(200)
        type("exit\n")

        withTimeout(TIMEOUT) { shutdownRequest.await() }
        assertEquals(listOf("crashed"), uncaught.map { it.message })
    }

    @Test
    fun `waits for a cancelled command but not for the blocked reader on stop`() = runBlocking {
        listener.start(scope)

        type("linger\n")
        withTimeout(TIMEOUT) { lingerStarted.await() }

        withTimeout(TIMEOUT) { listener.stop() }

        assertTrue(lingerCleanedUp.get(), "stop returned before the command finished")
    }

    @Test
    fun `stops after the timeout when a command ignores the cancellation`() = runBlocking {
        listener.start(scope)

        type("stuck\n")
        withTimeout(TIMEOUT) { stuckStarted.await() }

        // The lifecycle stops the endpoints in a non-cancellable context. Awaiting it from outside keeps withTimeout
        // effective, so a stop() that never returns fails the test instead of hanging it.
        val stop = scope.async { withContext(NonCancellable) { listener.stop() } }
        withTimeout(TIMEOUT) { stop.await() }
    }

    private fun type(text: String) {
        console.write(text.toByteArray())
        console.flush()
    }

    private fun command(name: String, block: suspend MicroserviceCommandContext.(List<String>) -> Unit) =
        object : MicroserviceCommand(name) {
            override suspend fun MicroserviceCommandContext.execute(args: List<String>) = block(args)
        }

    private companion object {
        val TIMEOUT = 5.seconds
        val STOP_TIMEOUT = 1.seconds
    }
}
