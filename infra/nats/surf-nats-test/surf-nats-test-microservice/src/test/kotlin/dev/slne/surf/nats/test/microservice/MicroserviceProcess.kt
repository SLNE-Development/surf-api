package dev.slne.surf.nats.test.microservice

import java.io.BufferedWriter
import java.nio.file.Path
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.io.path.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * A microservice jar running in its own JVM, driven through its console like an operator would.
 *
 * Every output line, including stderr, ends up in [output] in arrival order once it was consumed by [awaitLine] or
 * [awaitExit].
 */
class MicroserviceProcess private constructor(private val process: Process) : AutoCloseable {
    private val lines = LinkedBlockingQueue<String>()
    private val console: BufferedWriter = process.outputWriter()
    private val reader = thread(isDaemon = true, name = "microservice-output") {
        process.inputReader().forEachLine { line ->
            println("[microservice] $line")
            lines.put(line)
        }
    }

    val output: MutableList<String> = mutableListOf()

    /**
     * Types [command] into the console.
     */
    fun send(command: String) {
        console.write(command)
        console.newLine()
        console.flush()
    }

    /**
     * Waits for an output line containing [text] and returns it.
     *
     * @throws AssertionError if the line does not appear within [timeout] or the process exits first
     */
    fun awaitLine(text: String, timeout: Duration): String {
        val deadline = TimeSource.Monotonic.markNow() + timeout
        while (deadline.hasNotPassedNow()) {
            val line = lines.poll(POLL_MILLIS, TimeUnit.MILLISECONDS)
            if (line == null) {
                if (!reader.isAlive && lines.isEmpty()) break
                continue
            }
            output += line
            if (text in line) return line
        }
        throw AssertionError("No output line containing '$text' within $timeout")
    }

    /**
     * Waits for the process to exit, collects the remaining output and returns the exit code.
     */
    fun awaitExit(timeout: Duration): Int {
        check(process.waitFor(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)) {
            "The microservice did not exit within $timeout"
        }
        reader.join(READER_TIMEOUT.inWholeMilliseconds)
        lines.drainTo(output)
        return process.exitValue()
    }

    override fun close() {
        if (process.isAlive) {
            process.destroyForcibly().waitFor(READER_TIMEOUT.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        }
    }

    companion object {
        private const val POLL_MILLIS = 100L
        private val READER_TIMEOUT = 10.seconds

        /**
         * Starts [jar] with the Java runtime of this JVM in [workDir].
         */
        fun start(jar: Path, workDir: Path, environment: Map<String, String>): MicroserviceProcess {
            val java = ProcessHandle.current().info().command().map(::Path).orElseThrow()
            val process = ProcessBuilder(java.toString(), "-jar", jar.toString())
                .directory(workDir.toFile())
                .redirectErrorStream(true)
                .also { builder -> builder.environment().putAll(environment) }
                .start()
            return MicroserviceProcess(process)
        }
    }
}
