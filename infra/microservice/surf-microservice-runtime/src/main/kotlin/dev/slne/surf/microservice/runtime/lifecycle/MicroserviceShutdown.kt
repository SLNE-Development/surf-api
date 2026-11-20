package dev.slne.surf.microservice.runtime.lifecycle

import java.util.concurrent.CountDownLatch

/**
 * The shutdown request of the running microservice, such as the `exit` command or the close of its context.
 *
 * [request] may be called from any thread and any number of times; [awaitRequest] returns after the first call.
 *
 * The request does not say when the close of the context is over, because another thread than the waiting one may
 * close it: [awaitContextClosed] returns once [markContextClosed] ran.
 */
internal class MicroserviceShutdown {
    private val requested = CountDownLatch(1)
    private val contextClosed = CountDownLatch(1)

    /**
     * The exit code of the process once the shutdown completes.
     *
     * `0` unless `SpringApplication.exit` computed another one.
     */
    @Volatile
    var exitCode: Int = NORMAL_EXIT_CODE

    fun request() {
        requested.countDown()
    }

    fun awaitRequest() {
        requested.await()
    }

    /**
     * Signals that the context destroyed all its beans, the last step of a close that runs application code.
     */
    fun markContextClosed() {
        contextClosed.countDown()
    }

    fun awaitContextClosed() {
        contextClosed.await()
    }

    private companion object {
        const val NORMAL_EXIT_CODE = 0
    }
}
