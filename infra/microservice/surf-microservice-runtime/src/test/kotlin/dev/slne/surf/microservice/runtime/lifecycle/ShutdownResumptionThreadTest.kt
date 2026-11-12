package dev.slne.surf.microservice.runtime.lifecycle

import dev.slne.surf.microservice.api.Microservice
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.fail

/**
 * Covers the threading the launcher relies on: the code after [MicroserviceLifecycle.awaitShutdownRequest] calls
 * `exitProcess`, which blocks forever when it runs on the shutdown hook thread.
 */
class ShutdownResumptionThreadTest {

    @Test
    fun `runBlocking resumes the waiter on its own thread when the hook shuts down`() {
        val lifecycle = startedLifecycle()
        val resumedOn = CompletableFuture<Thread>()
        val launcher = thread(name = "launcher") {
            resumedOn.complete(runBlocking { awaitShutdownAndShutDown(lifecycle) })
        }
        awaitSuspended(launcher)

        shutDownOnHookThread(lifecycle)

        assertSame(launcher, resumedOn.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
    }

    @Test
    fun `a coroutine without a dispatcher resumes on the hook thread`() {
        // `suspend fun main` starts its body like this.
        val lifecycle = startedLifecycle()
        val resumedOn = CompletableFuture<Thread>()
        val body: suspend () -> Thread = { awaitShutdownAndShutDown(lifecycle) }
        body.startCoroutine(Continuation(EmptyCoroutineContext) { resumedOn.complete(it.getOrThrow()) })

        val hook = shutDownOnHookThread(lifecycle)

        assertSame(hook, resumedOn.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
    }

    private suspend fun awaitShutdownAndShutDown(lifecycle: MicroserviceLifecycle): Thread {
        lifecycle.awaitShutdownRequest()
        lifecycle.shutdown()
        return Thread.currentThread()
    }

    private fun shutDownOnHookThread(lifecycle: MicroserviceLifecycle): Thread {
        val hook = thread(name = "hook") { runBlocking { lifecycle.shutdown() } }
        hook.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS))
        assertFalse(hook.isAlive, "the shutdown hook did not finish")
        return hook
    }

    /**
     * Waits until [launcher] parks in its event loop, that is, until it suspended in `awaitShutdownRequest`.
     */
    private fun awaitSuspended(launcher: Thread) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS)
        while (launcher.state != Thread.State.WAITING && launcher.state != Thread.State.TIMED_WAITING) {
            if (System.nanoTime() > deadline) fail("the launcher thread did not suspend")
            Thread.sleep(10)
        }
    }

    private fun startedLifecycle(): MicroserviceLifecycle {
        val lifecycle = MicroserviceLifecycle(
            registerBuiltinCommands = {},
            createMicroservice = { TestMicroservice() },
            surfApi = NoOpComponent("surf-api"),
            spark = NoOpComponent("spark"),
            loadIntegrations = { emptyList() },
            commandEndpoints = NoOpComponent("endpoints"),
            shutdownLogging = {},
        )
        runBlocking { lifecycle.start(emptyList()) }
        return lifecycle
    }

    private class NoOpComponent(override val name: String) : RuntimeComponent {
        override suspend fun start() {}

        override suspend fun stop() {}
    }

    private class TestMicroservice : Microservice() {
        override val name = "test-service"
    }

    private companion object {
        const val TIMEOUT_SECONDS = 5L
    }
}
