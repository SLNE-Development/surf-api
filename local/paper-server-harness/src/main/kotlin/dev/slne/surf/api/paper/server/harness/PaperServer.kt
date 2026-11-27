package dev.slne.surf.api.paper.server.harness

import org.junit.jupiter.api.Assertions
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists

/**
 * Runs the server of a [ServerDirectory] as a child process, one run at a time, and collects the
 * console output of the current run.
 *
 * The server can be stopped, killed and started again on the same directory, and with it the same
 * world. Each [start] begins a new run: [lines], [mark] and the waits only see the output of that
 * run, while [consoleLog] keeps the output of every run of this instance.
 *
 * The output is collected as it arrives instead of being read back from a file afterwards, because
 * the interesting failures are the ones where the process never gets far enough to flush it. The
 * collected lines carry no terminal colour codes; [consoleLog] keeps them as the server wrote them.
 *
 * Not thread-safe: one test thread drives an instance.
 *
 * @param interesting substrings that make a log line worth quoting in a failure, such as plugin
 *   names and markers
 */
class PaperServer(
    val directory: ServerDirectory,
    private val javaLauncher: Path,
    private val jvmArgs: List<String>,
    private val environment: Map<String, String>,
    private val interesting: List<String> = emptyList(),
) : AutoCloseable {

    /**
     * The file every run of this instance appends its console output to.
     */
    val consoleLog: Path = directory.path.resolve(CONSOLE_LOG)

    private var lines: MutableList<String> = Collections.synchronizedList(mutableListOf())
    private var runs = 0

    private var process: Process? = null
    private var console: Writer? = null
    private var drain: Thread? = null

    /**
     * `true` while the process of the current run is alive.
     */
    val isRunning: Boolean get() = process?.isAlive == true

    /**
     * Starts a new run of the server.
     */
    fun start() {
        check(!isRunning) { "The server is already running." }

        if (runs == 0) {
            consoleLog.deleteIfExists()
        }

        runs++
        lines = Collections.synchronizedList(mutableListOf())

        val command = buildList {
            addAll(listOf(javaLauncher.absolutePathString(), "-Xmx2G"))
            addAll(jvmArgs)
            addAll(listOf("-jar", directory.serverJarName, "--nogui"))
        }

        println("Starting run $runs of ${directory.serverJarName} in ${directory.path}")

        val started = ProcessBuilder(command)
            .directory(directory.path.toFile())
            .redirectErrorStream(true)
            .apply { environment().putAll(environment) }
            .start()

        process = started
        console = started.outputWriter()
        drain = drainOutput(started, lines, "run $runs").apply { start() }
    }

    /**
     * Waits until the current run finished starting, or fails when it exits or takes longer than
     * [timeout].
     */
    fun awaitStarted(timeout: Duration = BOOT_TIMEOUT) {
        awaitLine("that it finished starting", timeout) { DONE in it }
    }

    /**
     * The number of lines the current run logged so far, for waits that must only see what follows.
     */
    fun mark(): Int = snapshot().size

    /**
     * Waits for a line of the current run, at or after line [from], that contains [marker].
     *
     * @return the first matching line
     */
    fun awaitLine(marker: String, timeout: Duration = GRACE, from: Int = 0): String {
        return awaitLine("a line containing '$marker'", timeout, from) { marker in it }
    }

    /**
     * Waits for a line of the current run, at or after line [from], that matches [predicate].
     *
     * Fails with the log when the process exits first or [timeout] passes. An exited process is
     * reported as what it is instead of as a timeout, because "the server died" and "the line
     * never appeared" send a reader to different parts of the log.
     *
     * @param description what the line says, completing "the server never logged ..."
     * @return the first matching line
     */
    fun awaitLine(
        description: String,
        timeout: Duration = GRACE,
        from: Int = 0,
        predicate: (String) -> Boolean,
    ): String {
        val deadline = System.nanoTime() + timeout.toNanos()

        while (System.nanoTime() < deadline) {
            findLine(from, predicate)?.let { return it }

            val current = checkNotNull(process) { "The server was never started." }

            if (!current.isAlive) {
                drain?.join(GRACE.toMillis())
                findLine(from, predicate)?.let { return it }

                fail(
                    "The server exited with code ${current.exitValue()} before it logged " +
                        "$description.",
                )
            }

            Thread.sleep(POLL.toMillis())
        }

        fail("The server never logged $description within ${timeout.toSeconds()}s.")
    }

    /**
     * Types [command] into the server console.
     */
    fun sendConsoleCommand(command: String) {
        val writer = checkNotNull(console) { "The server was never started." }

        writer.write(command + "\n")
        writer.flush()
    }

    /**
     * Sends `stop` and waits for the process to exit, killing it when it does not within
     * [SHUTDOWN_TIMEOUT].
     */
    fun stop() {
        val current = process ?: return

        if (current.isAlive) {
            runCatching { sendConsoleCommand("stop") }

            if (!current.waitFor(SHUTDOWN_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                current.destroyForcibly().waitFor(SHUTDOWN_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
            }
        }

        drain?.join(GRACE.toMillis())
    }

    /**
     * Kills the process without letting the server save or shut down, as a crash would.
     */
    fun kill() {
        val current = process ?: return

        current.destroyForcibly().waitFor(SHUTDOWN_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
        drain?.join(GRACE.toMillis())
    }

    override fun close() {
        stop()
    }

    /**
     * The lines of the current run.
     */
    fun lines(): List<String> = snapshot()

    /**
     * The lines of the current run that contain any of [markers].
     */
    fun linesContaining(markers: List<String>): List<String> {
        return snapshot().filter { line -> markers.any { it in line } }
    }

    /**
     * Fails the test with [message], followed by what the log of the current run says.
     */
    fun fail(message: String): Nothing = Assertions.fail(message + logExcerpt())

    private fun findLine(from: Int, predicate: (String) -> Boolean): String? {
        return snapshot().drop(from).firstOrNull(predicate)
    }

    private fun snapshot(): List<String> = synchronized(lines) { lines.toList() }

    private fun drainOutput(process: Process, sink: MutableList<String>, run: String): Thread {
        val drainer = Thread {
            Files.newBufferedWriter(
                consoleLog,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
            ).use { log ->
                log.write("---- $run of ${directory.serverJarName} ----\n")

                process.inputReader().forEachLine { line ->
                    sink += line.replace(ANSI_ESCAPE, "")
                    log.write(line)
                    log.write("\n")
                    log.flush()
                }
            }
        }

        drainer.isDaemon = true
        drainer.name = "paper-server-harness-log"

        return drainer
    }

    /**
     * What a failure has to carry to be worth reading.
     *
     * The lines matching [interesting] say what happened and are often nowhere near the end: a
     * plugin that refuses to load does so a stack trace and a world generation before the server
     * finishes starting. The tail catches whatever the test did not anticipate.
     */
    private fun logExcerpt(): String {
        val collected = snapshot()
        val relevant = collected.filter { line -> interesting.any { it in line } }
        val tail = collected.takeLast(TAIL_LINES)

        return buildString {
            append("\n\nThe server log is ")
            append(consoleLog)
            append(" (${collected.size} lines in this run).")

            if (relevant.isNotEmpty()) {
                append("\n\nWhat it says about the test's subjects:\n")
                append(relevant.take(RELEVANT_LINES).joinToString("\n") { "  | $it" })
            }

            append("\n\nIts last ${tail.size} lines:\n")
            append(tail.joinToString("\n") { "  | $it" })
        }
    }

    companion object {

        /**
         * Generous, because a first run downloads LuckPerms' own libraries as the server starts.
         */
        val BOOT_TIMEOUT: Duration = Duration.ofMinutes(5)

        /**
         * How long a wait for a single line lasts by default, and how far the log may lag the
         * process.
         */
        val GRACE: Duration = Duration.ofSeconds(10)

        val SHUTDOWN_TIMEOUT: Duration = Duration.ofSeconds(60)

        private val POLL: Duration = Duration.ofMillis(100)

        /**
         * The server's own "the server is up" line, which the plugins' enable phase precedes.
         */
        const val DONE = "Done ("

        private const val CONSOLE_LOG = "harness-console.log"

        /**
         * Terminal colour codes, which the server writes to its console even without a terminal.
         */
        private val ANSI_ESCAPE = Regex("\u001B\\[[0-9;]*[A-Za-z]")

        private const val TAIL_LINES = 80

        private const val RELEVANT_LINES = 40
    }
}
