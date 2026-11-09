package dev.slne.surf.api.paper.sharedbean.test

import dev.slne.surf.api.paper.sharedbean.consumer.SharedBeanReport
import dev.slne.surf.api.paper.sharedbean.provider.SharedGreeting
import dev.slne.surf.api.paper.sharedbean.provider.SharedGreetingImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions.abort
import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException
import java.io.Writer
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Duration
import java.util.Collections
import java.util.HexFormat
import java.util.Properties
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.fileSize
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.writeText

/**
 * Boots a real Paper server with surf-api and two plugins of its own, and proves that a
 * [dev.slne.surf.api.di.Scope.SHARED] bean declared by one of them reaches the other.
 *
 * Everything else in the repository tests the container against loaders built inside a test, which
 * is the only way to make two `Class` objects of one binary name exist on purpose — see
 * `ClassLoaderIsolationTest`. What no such test can exercise is the thing the design actually rests
 * on: real `PaperPluginClassLoader`s, built by a real server out of real plugin jars, with surf-api
 * sitting beside the plugins rather than above them. Two bootstrap defects reached a green build
 * because nothing here ever started a server, so this test starts one.
 *
 * ### What is asserted
 *
 * The consuming plugin takes the providing plugin's interface as a **constructor parameter** and
 * logs one line derived from the injected instance. Finding that line in the server's own log is
 * the whole result, and it is worth more than it looks:
 *
 * - the line exists at all, so the container resolved a bean that only the *other* plugin's
 *   container published, through the root
 * - it carries [SharedGreeting.GREETING], which only the providing plugin's implementation can
 *   produce, so the consumer made a real interface call on the injected object rather than on
 *   something of its own
 * - it says `sameLoader=true`, so the interface the consumer casts to and the one the instance
 *   implements are the same `Class` — the property `isAssignableFrom` turns on, and the one thing
 *   the container cannot check for itself because it matches providers to injection points by name
 *
 * ### Why it skips rather than fails
 *
 * Its inputs are a 55 MB server jar and three third-party plugin jars from two hosts. A missing
 * network, a moved release or a changed checksum is not a defect in this repository, and a test
 * that goes red for one of those teaches people to ignore it, so each case aborts with the reason.
 * Everything downstream of a successful assembly — a server that will not start, a plugin that will
 * not load, a missing line — fails, with the tail of the server log attached.
 *
 * ### Running it
 *
 * ```
 * ./gradlew :surf-api-paper-shared-bean-test:paperSharedBeanBootTest
 * ```
 *
 * It lives in a source set of its own and belongs to no lifecycle task, so neither `check`, nor
 * `test`, nor CI reaches it.
 */
class PaperSharedBeanBootTest {

    @Test
    fun `a shared bean crosses from one plugin to another on a real Paper server`() {
        val inputs = readInputs()
        val downloads = inputs.downloadsDir.createDirectories()

        val paperJar = inputs.download("paper", downloads)
        val thirdPartyPlugins = THIRD_PARTY_PLUGINS.map { inputs.download(it, downloads) }
        val ownPlugins = inputs.pluginJars()

        val serverDir = assembleServer(inputs, paperJar, thirdPartyPlugins + ownPlugins)
        val server = ServerRun(inputs.javaLauncher, serverDir, paperJar.name)

        val line = try {
            server.start()
            server.awaitReportLine()
        } finally {
            server.stop()
        }

        if (SharedGreeting.GREETING !in line) {
            bail(
                "The consumer logged its line, but not the greeting only the provider's " +
                        "implementation can produce, so it did not call across the plugin " +
                        "boundary:\n  | $line"
            )
        }

        if (SharedBeanReport.SAME_LOADER !in line) {
            bail(
                "The shared bean was injected, but the interface the consumer casts to is not the " +
                        "one the instance implements. Two loaders hold two unrelated types of the " +
                        "same binary name, and a bean shared across that boundary is useless to " +
                        "its consumer:\n  | $line"
            )
        }
    }

    // --- the server run -------------------------------------------------------------------------

    /**
     * One server process, its log, and the two things the test does with it: wait for a line, and
     * shut it down.
     *
     * The log is collected as it arrives rather than read back from the file afterwards, because
     * the interesting failures are the ones where the process never gets far enough to flush it.
     */
    private class ServerRun(
        private val javaLauncher: Path,
        private val directory: Path,
        private val paperJarName: String,
    ) {

        private val lines = Collections.synchronizedList(mutableListOf<String>())

        private lateinit var process: Process
        private lateinit var console: Writer

        /** Starts the process and the thread that drains its output. */
        fun start() {
            val logFile = directory.resolve("boot.log")

            logFile.deleteIfExists()

            process = ProcessBuilder(
                javaLauncher.absolutePathString(),
                "-Xmx2G",
                "-jar",
                paperJarName,
                "--nogui",
            ).directory(directory.toFile()).redirectErrorStream(true).start()

            console = process.outputWriter()

            Thread {
                Files.newBufferedWriter(
                    logFile,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND,
                ).use { sink ->
                    process.inputReader().forEachLine { line ->
                        lines += line
                        sink.write(line)
                        sink.write("\n")
                        sink.flush()
                    }
                }
            }.apply {
                isDaemon = true
                name = "paper-shared-bean-boot-test-log"
                start()
            }
        }

        /**
         * The consumer's report line, or a failure carrying the tail of the log.
         *
         * Three ways out, and the two unhappy ones are told apart on purpose. The server reaching
         * `Done (` without the line means the plugins' enable phase has already run and the line is
         * not coming, so there is nothing left to wait for and [GRACE] only covers the log being
         * written from more than one thread. A process that exits is reported as what it is rather
         * than as a timeout, because "the server died" and "the line never appeared" send a reader
         * to different parts of the log.
         */
        fun awaitReportLine(): String {
            val deadline = System.nanoTime() + BOOT_TIMEOUT.toNanos()
            var startedAt: Long? = null

            while (System.nanoTime() < deadline) {
                reportLine()?.let { return it }

                if (startedAt != null && System.nanoTime() - startedAt > GRACE.toNanos()) {
                    bail(
                        "The server finished starting and the consuming plugin never logged its " +
                                "line, so the shared bean never reached it. A plugin whose " +
                                "container cannot be built does not load at all, which the log " +
                                "says.${tail()}"
                    )
                }

                if (startedAt == null && snapshot().any { DONE in it }) {
                    startedAt = System.nanoTime()
                }

                if (!process.isAlive) {
                    // One more look: the draining thread may still be behind the process.
                    process.waitFor(GRACE.toMillis(), TimeUnit.MILLISECONDS)
                    reportLine()?.let { return it }

                    bail(
                        "The server exited with code ${process.exitValue()} before the consuming " +
                                "plugin logged its line.${tail()}"
                    )
                }

                Thread.sleep(POLL.toMillis())
            }

            bail(
                "The server did not finish starting within ${BOOT_TIMEOUT.toSeconds()}s and the " +
                        "consuming plugin never logged its line.${tail()}"
            )
        }

        /** Sends `stop`, and takes the process down if it does not go. */
        fun stop() {
            if (!::process.isInitialized) return

            runCatching {
                console.write("stop\n")
                console.flush()
            }

            if (!process.waitFor(SHUTDOWN_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly().waitFor(SHUTDOWN_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
            }
        }

        private fun reportLine() = snapshot().lastOrNull { SharedBeanReport.MARKER in it }

        private fun snapshot() = synchronized(lines) { lines.toList() }

        /**
         * What a failure has to carry to be worth reading.
         *
         * Two selections rather than one, because neither alone is enough. The lines about the
         * three plugins are the ones that say what happened and they are nowhere near the end — a
         * plugin that refuses to load does so a stack trace and a world generation before the
         * server finishes starting. The tail is what catches whatever this test did not anticipate.
         */
        private fun tail(): String {
            val collected = snapshot()
            val relevant = collected.filter { line -> INTERESTING.any { it in line } }
            val tail = collected.takeLast(TAIL_LINES)

            return buildString {
                append("\n\nThe server log is ")
                append(directory.resolve("boot.log"))
                append(" (${collected.size} lines).")

                if (relevant.isNotEmpty()) {
                    append("\n\nWhat it says about the plugins:\n")
                    append(relevant.take(RELEVANT_LINES).joinToString("\n") { "  | $it" })
                }

                append("\n\nIts last ${tail.size} lines:\n")
                append(tail.joinToString("\n") { "  | $it" })
            }
        }
    }

    // --- assembling the server ------------------------------------------------------------------

    /**
     * Lays out a server directory around [paperJar] with [plugins] installed, and returns it.
     *
     * Kept between runs rather than built in a temporary directory: Paper extracts its own
     * libraries on the first start and LuckPerms downloads a dozen of its own, which is most of
     * what a first run spends its time on and all of what a second run would otherwise spend it on
     * again. What is *not* kept is the plugins — every jar is removed and re-installed, so a run
     * can never pick up one a previous run left behind.
     */
    private fun assembleServer(inputs: Inputs, paperJar: Path, plugins: List<Path>): Path {
        val serverDir = inputs.workDir.resolve("server").createDirectories()
        val pluginsDir = serverDir.resolve("plugins").createDirectories()

        paperJar.copyTo(serverDir.resolve(paperJar.name), overwrite = true)
        serverDir.resolve("eula.txt").writeText("eula=true\n")
        serverDir.resolve("server.properties").writeText(serverProperties())

        pluginsDir.listDirectoryEntries("*.jar").forEach { it.deleteIfExists() }
        plugins.forEach { it.copyTo(pluginsDir.resolve(it.name), overwrite = true) }

        return serverDir
    }

    /**
     * The smallest `server.properties` that gets a server to its enable phase quickly.
     *
     * The port is one the operating system has just confirmed is free rather than a constant,
     * because whoever runs this test is quite likely to have a server of their own up.
     */
    private fun serverProperties(): String {
        val port = ServerSocket(0).use { it.localPort }

        return """
            enable-query=false
            enable-rcon=false
            level-name=world
            max-players=1
            motd=surf-api shared-bean boot test
            online-mode=false
            server-port=$port
            simulation-distance=2
            spawn-protection=0
            sync-chunk-writes=false
            view-distance=2
        """.trimIndent() + "\n"
    }

    // --- the test's inputs ----------------------------------------------------------------------

    /** What the build handed over: where to work, which java to use, and what to fetch. */
    private class Inputs(
        val workDir: Path,
        val javaLauncher: Path,
        private val values: Properties,
    ) {

        val downloadsDir: Path get() = workDir.resolve("downloads")

        /**
         * The jars built from this repository, as the build resolved them.
         *
         * A missing one aborts rather than fails, in the same spirit as a failed download: it means
         * the build did not produce what the test was to run, and the build has already said why.
         */
        fun pluginJars(): List<Path> {
            val declared = (values.getProperty("pluginJars") ?: "")
                .split(File.pathSeparatorChar)
                .filter { it.isNotBlank() }
                .map { Path.of(it) }

            if (declared.isEmpty()) {
                abort<Nothing>("The build resolved no plugin jars for the test to install.")
            }

            declared.filterNot { it.isRegularFile() }.takeIf { it.isNotEmpty() }?.let { missing ->
                abort<Nothing>(
                    "These plugin jars the build named do not exist: " +
                            missing.joinToString { it.absolutePathString() }
                )
            }

            return declared
        }

        /**
         * [id]'s pinned artifact in [into], fetched unless a byte-identical copy is already there.
         *
         * The checksum is what makes the cache safe as well as what makes the download pinned: a
         * half-written file from an interrupted run is replaced rather than reused, and an artifact
         * whose host has changed what it serves never reaches the server.
         */
        fun download(id: String, into: Path): Path {
            val fileName = property("download.$id.file")
            val url = property("download.$id.url")
            val expected = property("download.$id.sha256").lowercase()
            val target = into.resolve(fileName)

            if (target.isRegularFile() && sha256(target) == expected) return target

            val response = try {
                HTTP.send(
                    HttpRequest.newBuilder(URI.create(url))
                        .timeout(DOWNLOAD_TIMEOUT)
                        .GET()
                        .build(),
                    HttpResponse.BodyHandlers.ofFile(
                        target,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                    ),
                )
            } catch (failure: IOException) {
                abort<Nothing>("$fileName could not be downloaded from $url: $failure")
            } catch (failure: InterruptedException) {
                abort<Nothing>("The download of $fileName from $url was interrupted: $failure")
            }

            if (response.statusCode() != 200) {
                abort<Nothing>(
                    "$url answered ${response.statusCode()} instead of 200, so $fileName could " +
                            "not be obtained. A moved or withdrawn release is not a defect here."
                )
            }

            val actual = sha256(target)

            if (actual != expected) {
                abort<Nothing>(
                    "$fileName from $url has sha256 $actual, not the pinned $expected " +
                            "(${target.fileSize()} bytes). Either the host changed what it " +
                            "serves, or the version was bumped without its checksum."
                )
            }

            return target
        }

        private fun property(key: String) = values.getProperty(key)
            ?: abort<Nothing>("The build's input file has no '$key'.")
    }

    /** Reads the file the build wrote, or aborts saying the build never wrote it. */
    private fun readInputs(): Inputs {
        val inputsFile = systemPath("surf.sharedBeanBootTest.inputs")
        val workDir = systemPath("surf.sharedBeanBootTest.workDir")
        val javaLauncher = systemPath("surf.sharedBeanBootTest.java")

        if (!inputsFile.isRegularFile()) {
            abort<Nothing>("The build's input file $inputsFile does not exist.")
        }

        if (!javaLauncher.isRegularFile()) {
            abort<Nothing>("The java launcher $javaLauncher the build named does not exist.")
        }

        return Inputs(
            workDir = workDir.createDirectories(),
            javaLauncher = javaLauncher,
            values = Properties().also { properties ->
                Files.newBufferedReader(inputsFile).use { properties.load(it) }
            },
        )
    }

    private fun systemPath(key: String) = System.getProperty(key)
        ?.let(Path::of)
        ?: abort<Nothing>(
            "-D$key is not set, so this test was not started by its own Gradle task. Run " +
                    "./gradlew :surf-api-paper-shared-bean-test:paperSharedBeanBootTest"
        )

    private companion object {

        /** Follows redirects, which both release hosts use. */
        val HTTP: HttpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build()

        val DOWNLOAD_TIMEOUT: Duration = Duration.ofMinutes(10)

        /** Generous, because a first run downloads LuckPerms' own libraries as the server starts. */
        val BOOT_TIMEOUT: Duration = Duration.ofMinutes(5)

        /** How far the log is allowed to lag the process. */
        val GRACE: Duration = Duration.ofSeconds(10)

        val POLL: Duration = Duration.ofMillis(250)

        val SHUTDOWN_TIMEOUT: Duration = Duration.ofSeconds(60)

        /** Paper's own "the server is up" line, which the plugins' enable phase precedes. */
        const val DONE = "Done ("

        const val TAIL_LINES = 80

        const val RELEVANT_LINES = 40

        /**
         * What makes a log line worth quoting in a failure.
         *
         * The three plugin names and the two markers, so a reader can see which containers got as
         * far as running their beans; and the container's own exception type, which is what a
         * plugin whose dependencies cannot be satisfied fails with.
         */
        val INTERESTING = listOf(
            "surf-paper-api",
            "SurfSharedBeanProvider",
            "SurfSharedBeanConsumer",
            SharedBeanReport.MARKER,
            SharedGreetingImpl.PROVIDER_MARKER,
            "UnsatisfiedDependencyException",
        )

        /** The ids of the three plugins surf-api's own `paper-plugin.yml` requires. */
        val THIRD_PARTY_PLUGINS = listOf("commandapi", "luckperms", "packetevents")

        fun sha256(file: Path): String = HexFormat.of()
            .formatHex(MessageDigest.getInstance("SHA-256").digest(file.readBytes()))

        /** Fails the test with [message], which is the only thing a reader of a red run gets. */
        fun bail(message: String): Nothing = Assertions.fail(message)
    }
}
