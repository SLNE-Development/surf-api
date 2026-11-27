package dev.slne.surf.api.velocity.sharedbean.test

import dev.slne.surf.api.velocity.sharedbean.consumer.SharedBeanReport
import dev.slne.surf.api.velocity.sharedbean.provider.SharedGreeting
import dev.slne.surf.api.velocity.sharedbean.provider.SharedGreetingImpl
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
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.io.path.*

/**
 * Boots a real Velocity proxy with surf-api and two plugins of its own, and proves that a
 * `@PluginApi` bean exported by one of them reaches the other through surf-api's Spring contexts.
 *
 * The unit tests of `surf-api-spring-impl` build plugin contexts inside one JVM class loader. What
 * they cannot exercise is the thing the design rests on: real `PluginClassLoader`s, built by a
 * real proxy out of real plugin jars, contexts built when Guice injects the plugins, and surf-api
 * sitting beside the plugins rather than above them.
 *
 * ### What is asserted
 *
 * The consuming plugin takes the providing plugin's interface as a **constructor parameter** and
 * logs one report line derived from the injected instance:
 *
 * - it carries [SharedGreeting.GREETING], which only the providing plugin's implementation can
 *   produce, so the consumer made a real interface call on the injected object
 * - it says `sameLoader=true`, so the interface the consumer casts to and the one the instance
 *   implements are the same `Class`
 * - the remaining fields cover what [EXPECTED_FIELDS] lists: private beans stay private, the plugin
 *   instance and container resolve, the context class loader, `@ConditionalOnPlugin`, CGLIB
 *   configuration classes, the Velocity and surf event listeners and the absence of JVM shutdown
 *   hooks
 *
 * After the proxy stopped, the phase lines of both plugins must appear in [EXPECTED_PHASES]
 * order: load and enable provider first, disable consumer first, and the export destroyed once, by
 * its provider.
 *
 * ### Why it skips rather than fails
 *
 * Its inputs are a 40 MB proxy jar and three third-party plugin jars from three hosts. A missing
 * network, a moved release or a changed checksum is not a defect in this repository, and a test
 * that goes red for one of those teaches people to ignore it, so each case aborts with the reason.
 * Everything downstream of a successful assembly -- a proxy that will not start, a plugin that will
 * not load, a missing line -- fails, with the tail of the proxy log attached.
 *
 * ### Running it
 *
 * ```
 * ./gradlew :surf-api-velocity-shared-bean-test:velocitySharedBeanBootTest
 * ```
 *
 * `-Psurf.bootTest.jvmArgs="<args>"` adds whitespace-separated JVM arguments to the test JVM, which
 * downloads, and to the proxy process, for example a trust-store setting for a machine behind a
 * TLS-intercepting proxy.
 *
 * It lives in a source set of its own and belongs to no lifecycle task, so neither `check`, nor
 * `test`, nor CI reaches it.
 */
class VelocitySharedBeanBootTest {

    @Test
    fun `a shared bean crosses from one plugin to another on a real Velocity proxy`() {
        val inputs = readInputs()
        val downloads = inputs.downloadsDir.createDirectories()

        val velocityJar = inputs.download("velocity", downloads)
        val thirdPartyPlugins = THIRD_PARTY_PLUGINS.map { inputs.download(it, downloads) }
        val ownPlugins = inputs.pluginJars()

        val proxyDir = assembleProxy(inputs, velocityJar, thirdPartyPlugins + ownPlugins)
        val proxy = ProxyRun(
            inputs.javaLauncher,
            proxyDir,
            velocityJar.name,
            inputs.jvmArgs,
            freePort(),
        )

        val line = try {
            proxy.start()
            proxy.awaitReportLine().also {
                // The command bean is run from the console. It proves that surf-api registered the
                // command in the enable phase.
                proxy.sendConsoleCommand(SharedBeanReport.COMMAND)
                proxy.awaitLine(COMMAND_LINE)
            }
        } finally {
            proxy.stop()
        }

        if (SharedGreeting.GREETING !in line) {
            bail(
                "The consumer logged its line, but not the greeting only the provider's " +
                    "implementation can produce, so it did not call across the plugin " +
                    "boundary:\n  | $line",
            )
        }

        val missing = EXPECTED_FIELDS.filterNot { it in line }

        if (missing.isNotEmpty()) {
            bail("The consumer's report line lacks $missing:\n  | $line")
        }

        val failures = proxy.linesContaining(FAILURE_MARKERS)

        if (failures.isNotEmpty()) {
            bail("A surf plugin's context failed:\n" + failures.joinToString("\n") { "  | $it" })
        }

        val phases = proxy.phases()

        if (phases != EXPECTED_PHASES) {
            bail(
                "The plugins ran their lifecycle phases out of order.\n" +
                    "Expected: $EXPECTED_PHASES\nActual:   $phases",
            )
        }
    }

    /**
     * One proxy process, its log, and the two things the test does with it: wait for a line, and
     * shut it down.
     *
     * The log is collected as it arrives rather than read back from the file afterwards, because
     * the interesting failures are the ones where the process never gets far enough to flush it.
     */
    private class ProxyRun(
        private val javaLauncher: Path,
        private val directory: Path,
        private val velocityJarName: String,
        private val extraJvmArgs: List<String>,
        private val port: Int,
    ) {

        private val lines = Collections.synchronizedList(mutableListOf<String>())

        private lateinit var process: Process
        private lateinit var console: Writer

        /**
         * Starts the process and the thread that drains its output.
         */
        fun start() {
            val logFile = directory.resolve("boot.log")

            logFile.deleteIfExists()

            val command = listOf(javaLauncher.absolutePathString(), "-Xmx1G") +
                extraJvmArgs + listOf("-jar", velocityJarName, "--port", port.toString())

            process = ProcessBuilder(command)
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .start()

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
                name = "velocity-shared-bean-boot-test-log"
                start()
            }
        }

        /**
         * The consumer's report line, or a failure carrying the tail of the log.
         *
         * Three ways out, and the two unhappy ones are told apart on purpose. The proxy reaching
         * `Done (` without the line means the plugins' enable phase has already run and the line is
         * not coming, so there is nothing left to wait for and [GRACE] only covers the log being
         * written from more than one thread. A process that exits is reported as what it is rather
         * than as a timeout, because "the proxy died" and "the line never appeared" send a reader
         * to different parts of the log.
         */
        fun awaitReportLine(): String {
            val deadline = System.nanoTime() + BOOT_TIMEOUT.toNanos()
            var startedAt: Long? = null

            while (System.nanoTime() < deadline) {
                reportLine()?.let { return it }

                if (startedAt != null && System.nanoTime() - startedAt > GRACE.toNanos()) {
                    bail(
                        "The proxy finished starting and the consuming plugin never logged its " +
                            "line, so the shared bean never reached it. A plugin whose " +
                            "context cannot be built does not load at all, which the log " +
                            "says.${tail()}",
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
                        "The proxy exited with code ${process.exitValue()} before the consuming " +
                            "plugin logged its line.${tail()}",
                    )
                }

                Thread.sleep(POLL.toMillis())
            }

            bail(
                "The proxy did not finish starting within ${BOOT_TIMEOUT.toSeconds()}s and the " +
                    "consuming plugin never logged its line.${tail()}",
            )
        }

        /**
         * Types [command] into the proxy console.
         */
        fun sendConsoleCommand(command: String) {
            console.write(command + "\n")
            console.flush()
        }

        /**
         * Waits up to [GRACE] for a log line containing [marker], or fails with the tail of the
         * log.
         */
        fun awaitLine(marker: String) {
            val deadline = System.nanoTime() + GRACE.toNanos()

            while (System.nanoTime() < deadline) {
                if (snapshot().any { marker in it }) return

                Thread.sleep(POLL.toMillis())
            }

            bail("The proxy never logged a line containing '$marker'.${tail()}")
        }

        /**
         * Sends `end`, and takes the process down if it does not go.
         */
        fun stop() {
            if (!::process.isInitialized) return

            runCatching {
                console.write("end\n")
                console.flush()
            }

            if (!process.waitFor(SHUTDOWN_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly().waitFor(SHUTDOWN_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
            } else {
                // The draining thread may still be behind the process, and the phase lines of the
                // shutdown are the last ones written.
                Thread.sleep(GRACE.toMillis() / 5)
            }
        }

        private fun reportLine() = snapshot().lastOrNull { SharedBeanReport.MARKER in it }

        /**
         * The log lines that contain any of [markers].
         */
        fun linesContaining(markers: List<String>): List<String> {
            return snapshot().filter { line -> markers.any { it in line } }
        }

        /**
         * The phases both test plugins logged, as `provider:load` and the like, in log order.
         */
        fun phases(): List<String> {
            return snapshot()
                .mapNotNull { PHASE_LINE.find(it) }
                .map { "${it.groupValues[1]}:${it.groupValues[2]}" }
        }

        private fun snapshot() = synchronized(lines) { lines.toList() }

        /**
         * What a failure has to carry to be worth reading.
         *
         * Two selections rather than one, because neither alone is enough. The lines about the
         * plugins are the ones that say what happened and they are nowhere near the end -- a plugin
         * that refuses to load does so a stack trace before the proxy finishes starting. The tail
         * is what catches whatever this test did not anticipate.
         */
        private fun tail(): String {
            val collected = snapshot()
            val relevant = collected.filter { line -> INTERESTING.any { it in line } }
            val tail = collected.takeLast(TAIL_LINES)

            return buildString {
                append("\n\nThe proxy log is ")
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

    /**
     * Lays out a proxy directory around [velocityJar] with [plugins] installed, and returns it.
     *
     * Kept between runs rather than built in a temporary directory: LuckPerms downloads a dozen
     * libraries of its own on its first start, which is most of what a first run spends its time on
     * and all of what a second run would otherwise spend it on again. What is *not* kept is the
     * plugins -- every jar is removed and re-installed, so a run can never pick up one a previous
     * run left behind. Velocity writes its default configuration on the first start; the port is
     * passed on the command line.
     */
    private fun assembleProxy(inputs: Inputs, velocityJar: Path, plugins: List<Path>): Path {
        val proxyDir = inputs.workDir.resolve("proxy").createDirectories()
        val pluginsDir = proxyDir.resolve("plugins").createDirectories()

        velocityJar.copyTo(proxyDir.resolve(velocityJar.name), overwrite = true)

        for (jar in pluginsDir.listDirectoryEntries("*.jar")) {
            jar.deleteIfExists()
        }

        for (plugin in plugins) {
            plugin.copyTo(pluginsDir.resolve(plugin.name), overwrite = true)
        }

        return proxyDir
    }

    /**
     * A port the operating system has just confirmed is free rather than a constant, because
     * whoever runs this test is quite likely to have a proxy of their own up.
     */
    private fun freePort(): Int = ServerSocket(0).use { it.localPort }

    /**
     * What the build handed over: where to work, which java to use, which extra JVM arguments the
     * proxy process takes, and what to fetch.
     */
    private class Inputs(
        val workDir: Path,
        val javaLauncher: Path,
        val jvmArgs: List<String>,
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
                        missing.joinToString { it.absolutePathString() },
                )
            }

            return declared
        }

        /**
         * [id]'s pinned artifact in [into], fetched unless a byte-identical copy is already there.
         *
         * The checksum is what makes the cache safe as well as what makes the download pinned: a
         * half-written file from an interrupted run is replaced rather than reused, and an artifact
         * whose host has changed what it serves never reaches the proxy.
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
                        "not be obtained. A moved or withdrawn release is not a defect here.",
                )
            }

            val actual = sha256(target)

            if (actual != expected) {
                abort<Nothing>(
                    "$fileName from $url has sha256 $actual, not the pinned $expected " +
                        "(${target.fileSize()} bytes). Either the host changed what it " +
                        "serves, or the version was bumped without its checksum.",
                )
            }

            return target
        }

        private fun property(key: String) = values.getProperty(key)
            ?: abort<Nothing>("The build's input file has no '$key'.")
    }

    /**
     * Reads the file the build wrote, or aborts saying the build never wrote it.
     */
    private fun readInputs(): Inputs {
        val inputsFile = systemPath("surf.velocityBootTest.inputs")
        val workDir = systemPath("surf.velocityBootTest.workDir")
        val javaLauncher = systemPath("surf.velocityBootTest.java")

        if (!inputsFile.isRegularFile()) {
            abort<Nothing>("The build's input file $inputsFile does not exist.")
        }

        if (!javaLauncher.isRegularFile()) {
            abort<Nothing>("The java launcher $javaLauncher the build named does not exist.")
        }

        return Inputs(
            workDir = workDir.createDirectories(),
            javaLauncher = javaLauncher,
            jvmArgs = System.getProperty("surf.velocityBootTest.jvmArgs").orEmpty()
                .split(Regex("\\s+"))
                .filter { it.isNotEmpty() },
            values = Properties().also { properties ->
                Files.newBufferedReader(inputsFile).use { properties.load(it) }
            },
        )
    }

    private fun systemPath(key: String) = System.getProperty(key)
        ?.let(Path::of)
        ?: abort<Nothing>(
            "-D$key is not set, so this test was not started by its own Gradle task. Run " +
                "./gradlew :surf-api-velocity-shared-bean-test:velocitySharedBeanBootTest",
        )

    private companion object {

        /**
         * Follows redirects, which all three release hosts use.
         */
        val HTTP: HttpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build()

        val DOWNLOAD_TIMEOUT: Duration = Duration.ofMinutes(10)

        /**
         * Generous, because a first run downloads LuckPerms' own libraries as the proxy starts.
         */
        val BOOT_TIMEOUT: Duration = Duration.ofMinutes(5)

        /**
         * How far the log is allowed to lag the process.
         */
        val GRACE: Duration = Duration.ofSeconds(10)

        val POLL: Duration = Duration.ofMillis(250)

        val SHUTDOWN_TIMEOUT: Duration = Duration.ofSeconds(60)

        /**
         * Velocity's own "the proxy is up" line, which the plugins' enable phase precedes.
         */
        const val DONE = "Done ("

        const val TAIL_LINES = 80

        const val RELEVANT_LINES = 40

        /**
         * What makes a log line worth quoting in a failure.
         *
         * The plugin ids and the two markers, so a reader can see which contexts got as far as
         * running their beans; and the context manager's own exception type, which is what a plugin
         * whose dependencies cannot be satisfied fails with.
         */
        val INTERESTING = listOf(
            "surf-api-velocity",
            "surf-shared-bean-provider",
            SharedBeanReport.PLUGIN_ID,
            SharedBeanReport.MARKER,
            SharedGreetingImpl.PROVIDER_MARKER,
            "UnsatisfiedDependencyException",
            "PluginContextException",
        )

        /**
         * The fields of the consumer's report line as they read when every expectation holds.
         *
         * The provider's internals stay invisible, the plugin instance and container resolve to the
         * consumer's own, the context class loader is the plugin's during bean creation, only the
         * LuckPerms adapter exists because the absent plugin's adapter is skipped, configuration
         * classes get their CGLIB subclass, the suspending Velocity listener and the surf event
         * listener both received their event while the listener marked with
         * `@ManualListenerRegistration` did not, no context registered a JVM shutdown hook, and
         * Spring logs through the proxy's own logging. Spring picks Log4j 2 first when it finds it, and
         * Velocity runs on it. The plugin instance had finished its constructor before its context
         * was built, the plugin's logger bean is named after the plugin, and Guice injects the main
         * class's own `@Inject` field and method only after the load phase, as `SurfVelocityPlugin`
         * documents.
         */
        val EXPECTED_FIELDS = listOf(
            "sameLoader=true",
            "privateVisible=false",
            "pluginIsInstance=true",
            "containerIsOwn=true",
            "tccl=true",
            "probes=[LuckPermsProbe]",
            "configProxy=true",
            "surfEvent=true",
            "velocityEvent=true",
            "manualVelocityEvent=false",
            "shutdownHooks=0",
            "logImpl=Log4j2Log",
            "pluginConstructed=true",
            "pluginLogger=${SharedBeanReport.PLUGIN_ID}",
            "subclassField=false",
            "subclassMethod=false",
        )

        /**
         * What the consumer's command bean logs when the console runs it.
         */
        val COMMAND_LINE = "${SharedBeanReport.SPIKE_MARKER} command=executed"

        /**
         * Log lines that mean a surf plugin's context failed somewhere, even if the report
         * appeared.
         */
        val FAILURE_MARKERS = listOf(
            "PluginContextException",
            "failed to enable",
            "Can't create plugin",
        )

        /**
         * A phase line of either test plugin; group 1 is the plugin role, group 2 the phase.
         */
        val PHASE_LINE = Regex(
            """${SharedBeanReport.SPIKE_MARKER} (provider|consumer) phase=(\w+)""",
        )

        /**
         * The phases in the order the design promises: load and enable go provider first, disable
         * goes consumer first, and the export is destroyed once, by its provider, last.
         */
        val EXPECTED_PHASES = listOf(
            "provider:load", "consumer:load",
            "provider:enable", "consumer:enable",
            "consumer:disable", "provider:disable",
            "provider:destroy",
        )

        /**
         * The ids of the three plugins surf-api's own Velocity plugin requires.
         */
        val THIRD_PARTY_PLUGINS = listOf("commandapi", "luckperms", "packetevents")

        fun sha256(file: Path): String {
            return HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-256").digest(file.readBytes()))
        }

        /**
         * Fails the test with [message], which is the only thing a reader of a red run gets.
         */
        fun bail(message: String): Nothing = Assertions.fail(message)
    }
}
