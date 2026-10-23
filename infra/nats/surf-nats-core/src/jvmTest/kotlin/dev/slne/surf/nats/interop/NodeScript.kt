package dev.slne.surf.nats.interop

import dev.slne.surf.nats.TestNatsServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * One run of `interop.mjs <scenario>` on Node, started by [start].
 *
 * The script reports on stdout with marker lines: `@ready <step>` once the JVM may act, `@check <name>` and
 * `@vector <name>` for passed checks. Every other line, and stderr, only goes to the failure output.
 */
internal class NodeScript private constructor(private val scenario: String, private val process: Process) :
    AutoCloseable {

    private val markers = LinkedBlockingQueue<String>()
    private val output = StringBuffer()
    private val checks = mutableListOf<String>()
    private val vectors = mutableListOf<String>()

    private val readers = listOf(
        thread(isDaemon = true, name = "node-$scenario-stdout") {
            process.inputStream.bufferedReader().forEachLine { line ->
                output.appendLine(line)
                if (line.startsWith(MARKER)) markers.put(line)
            }
            markers.put(END)
        },
        thread(isDaemon = true, name = "node-$scenario-stderr") {
            process.errorStream.bufferedReader().forEachLine { output.appendLine("[stderr] $it") }
        },
    )

    /**
     * Waits for `@ready <step>`; checks reported before it are kept for [awaitSuccess].
     */
    suspend fun awaitReady(step: String, timeout: Duration = 30.seconds) = runInterruptible(Dispatchers.IO) {
        val deadline = System.nanoTime() + timeout.inWholeNanoseconds
        while (true) {
            val marker = markers.poll(deadline - System.nanoTime(), TimeUnit.NANOSECONDS)
                ?: fail("node $scenario did not report ready $step within $timeout\n$output")
            if (marker == END) fail("node $scenario ended before ready $step\n$output")
            if (marker == "$MARKER$READY $step") return@runInterruptible
            record(marker)
        }
    }

    /**
     * Waits for the script to exit with 0 and returns the passed checks and vectors in report order.
     */
    suspend fun awaitSuccess(timeout: Duration = 2.minutes): NodeResult = runInterruptible(Dispatchers.IO) {
        if (!process.waitFor(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
            fail("node $scenario did not exit within $timeout\n$output")
        }
        readers.forEach { it.join(READER_JOIN_MILLIS) }
        generateSequence { markers.poll() }.filter { it != END }.forEach(::record)
        assertEquals(0, process.exitValue(), "node $scenario failed\n$output")
        NodeResult(checks.toList(), vectors.toList())
    }

    private fun record(marker: String) {
        val kind = marker.removePrefix(MARKER).substringBefore(' ')
        val name = marker.substringAfter(' ')
        when (kind) {
            CHECK -> checks += name
            VECTOR -> vectors += name
            else -> fail("node $scenario reported an unexpected marker '$marker'\n$output")
        }
    }

    override fun close() {
        if (process.isAlive) process.destroyForcibly()
    }

    class NodeResult(val checks: List<String>, val vectors: List<String>)

    companion object {
        private const val MARKER = "@"
        private const val READY = "ready"
        private const val CHECK = "check"
        private const val VECTOR = "vector"
        private const val END = "@end"
        private const val READER_JOIN_MILLIS = 5_000L

        private val directory by lazy {
            File(requireNotNull(System.getProperty("surf.nats.interop.dir")) {
                "surf.nats.interop.dir is not set: run JsInteropTest with ./gradlew :surf-nats-core:jvmInteropTest"
            })
        }

        private val node by lazy {
            requireNotNull(System.getProperty("surf.nats.interop.node")) {
                "surf.nats.interop.node is not set: run JsInteropTest with ./gradlew :surf-nats-core:jvmInteropTest"
            }
        }

        /**
         * Starts the script with `SURF_NATS_SERVERS` pointing to [TestNatsServer] plus [environment].
         */
        fun start(scenario: String, environment: Map<String, String> = emptyMap()): NodeScript {
            val builder = ProcessBuilder(node, "interop.mjs", scenario).directory(directory)
            builder.environment().putAll(environment)
            // nats.js may resolve localhost to ::1 first
            builder.environment()["SURF_NATS_SERVERS"] = TestNatsServer.url.replace("localhost", "127.0.0.1")
            return NodeScript(scenario, builder.start())
        }
    }
}
