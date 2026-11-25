package dev.slne.surf.api.paper.sharedbean.test

import dev.slne.surf.api.paper.sharedbean.consumer.SharedBeanReport
import dev.slne.surf.api.paper.sharedbean.provider.SharedGreeting
import dev.slne.surf.api.paper.sharedbean.provider.SharedGreetingImpl
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions.abort
import org.junit.jupiter.api.Test
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
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
 * Boots a real Paper server with surf-api and two plugins of its own, and proves that a
 * `@PluginApi` bean exported by one of them reaches the other through surf-api's Spring contexts.
 *
 * The unit tests of `surf-api-spring-impl` build plugin contexts inside one JVM class loader. What
 * they cannot exercise is the thing the design rests on: real `PaperPluginClassLoader`s, built by a
 * real server out of real plugin jars, contexts built in Paper's bootstrap phase, and surf-api
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
 *   instance resolves, the context class loader, `@ConditionalOnPlugin`, CGLIB configuration
 *   classes and the absence of JVM shutdown hooks
 * - `scopeSurvivesLoad` says that a coroutine launched in `onLoad` outlived the load phase and
 *   reached the server thread, which the enable hook waited for
 * - `surfConfig` is read through the `ConfigHandle` surf-api registered for the consumer's
 *   `@SurfConfig` class, from a file in a subfolder of its data folder
 * - the suspending command logs [EXPECTED_SCOPE_COMMAND_FIELDS]: its default scope is the
 *   consumer's own, and it starts on the server thread
 * - `redis` and `redisson` say that the consumer's injected `RedisApi` wrote a sync value and read
 *   it back from Redis, and that the injected `RedissonClient` proxy forwarded a command, both
 *   after surf-api connected the plugin
 * - the NATS command logs [EXPECTED_NATS_COMMAND_FIELDS]: a call through a service proxy reached
 *   the consumer's own `@NatsService` over the broker, a published event reached its
 *   `@NatsListener`, and an `ItemStack` survived the surf-nats serializer round trip on the server
 * - after `stop`, the disable hook logs `disableJoin=returned`: it cancelled and joined a
 *   coroutine that was suspended on server thread work while that thread was blocked in the hook
 *
 * Redis and NATS run in Docker containers the test starts, and the server reads their addresses
 * from the environment variables of surf-api's configuration. A machine without Docker skips the
 * test.
 *
 * After the server stopped, the phase lines of both plugins must appear in [EXPECTED_PHASES]
 * order: bootstrap, load and enable provider first, disable consumer first, and the export
 * destroyed once, by its provider.
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
 * `-Psurf.bootTest.jvmArgs="<args>"` adds whitespace-separated JVM arguments to the test JVM, which
 * downloads, and to the server process, for example a trust-store setting for a machine behind a
 * TLS-intercepting proxy.
 *
 * It lives in a source set of its own and belongs to no lifecycle task, so neither `check`, nor
 * `test`, nor CI reaches it.
 */
class PaperSharedBeanBootTest {

    private var startedBrokers: BrokerContainers? = null

    @AfterEach
    fun stopBrokers() {
        startedBrokers?.close()
    }

    @Test
    fun `a shared bean crosses from one plugin to another on a real Paper server`() {
        val inputs = readInputs()
        val downloads = inputs.downloadsDir.createDirectories()

        val paperJar = inputs.download("paper", downloads)
        val thirdPartyPlugins = THIRD_PARTY_PLUGINS.map { inputs.download(it, downloads) }
        val ownPlugins = inputs.pluginJars()

        val serverDir = assembleServer(inputs, paperJar, thirdPartyPlugins + ownPlugins)

        val brokers = BrokerContainers.start().also { startedBrokers = it }
        val server = ServerRun(
            inputs.javaLauncher,
            serverDir,
            paperJar.name,
            inputs.jvmArgs,
            brokers.environment,
        )

        val line = try {
            server.start()
            server.awaitReportLine().also {
                // The listener bean reacts to the end of startup; the command beans are run from
                // the console. All prove that surf-api registered the beans in the enable phase.
                server.awaitLine(LISTENER_LINE)
                server.sendConsoleCommand(SharedBeanReport.COMMAND)
                server.awaitLine(COMMAND_LINE)
                server.sendConsoleCommand(SharedBeanReport.SCOPE_COMMAND)
                server.awaitLine(SCOPE_COMMAND_LINE)
                server.sendConsoleCommand(SharedBeanReport.NATS_COMMAND)
                server.awaitLine(NATS_COMMAND_LINE)
            }
        } finally {
            server.stop()
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

        // The suspending command's own line: it is logged after the report, so it is not part of
        // it.
        val scopeLine = server.linesContaining(listOf(SCOPE_COMMAND_LINE)).last()
        val missingScopeFields = EXPECTED_SCOPE_COMMAND_FIELDS.filterNot { it in scopeLine }

        if (missingScopeFields.isNotEmpty()) {
            bail("The suspending command's line lacks $missingScopeFields:\n  | $scopeLine")
        }

        // The NATS round trips run from the console after the enable phase, so they log a line of
        // their own as well.
        val natsLine = server.linesContaining(listOf(NATS_COMMAND_LINE)).last()
        val missingNatsFields = EXPECTED_NATS_COMMAND_FIELDS.filterNot { it in natsLine }

        if (missingNatsFields.isNotEmpty()) {
            bail("The NATS command's line lacks $missingNatsFields:\n  | $natsLine")
        }

        // Logged by the consumer's disable hook, so it can only appear once the server stopped.
        server.awaitLine(DISABLE_JOIN_MARKER)

        if (server.linesContaining(listOf(DISABLE_JOIN_LINE)).isEmpty()) {
            val seen = server.linesContaining(listOf(DISABLE_JOIN_MARKER))
            bail(
                "The disable hook did not get its coroutine joined while the server thread was " +
                    "blocked:\n" + seen.joinToString("\n") { "  | $it" },
            )
        }

        val failures = server.linesContaining(FAILURE_MARKERS)

        if (failures.isNotEmpty()) {
            bail("A surf plugin's context failed:\n" + failures.joinToString("\n") { "  | $it" })
        }

        val phases = server.phases()

        if (phases != EXPECTED_PHASES) {
            bail(
                "The plugins ran their lifecycle phases out of order.\n" +
                    "Expected: $EXPECTED_PHASES\nActual:   $phases",
            )
        }
    }

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
        private val extraJvmArgs: List<String>,
        private val environment: Map<String, String>,
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

            val command = buildList {
                addAll(listOf(javaLauncher.absolutePathString(), "-Xmx2G"))
                addAll(extraJvmArgs)
                addAll(listOf("-jar", paperJarName, "--nogui"))
            }

            process = ProcessBuilder(command)
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .apply { environment().putAll(environment) }
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
                            "plugin context cannot be built does not load at all, which the log " +
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
                        "The server exited with code ${process.exitValue()} before the consuming " +
                            "plugin logged its line.${tail()}",
                    )
                }

                Thread.sleep(POLL.toMillis())
            }

            bail(
                "The server did not finish starting within ${BOOT_TIMEOUT.toSeconds()}s and the " +
                    "consuming plugin never logged its line.${tail()}",
            )
        }

        /**
         * Types [command] into the server console.
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

            bail("The server never logged a line containing '$marker'.${tail()}")
        }

        /**
         * Sends `stop`, and takes the process down if it does not go.
         */
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

    /**
     * The Redis and NATS the server connects to, each in a Docker container of its own.
     *
     * [environment] carries their addresses under the variable names surf-api's configuration
     * reads, so the server needs no configuration file. A machine without Docker aborts the test
     * with the reason: it is not a defect in this repository, in the spirit of the failed
     * downloads.
     */
    private class BrokerContainers private constructor(
        private val redis: GenericContainer<*>,
        private val nats: GenericContainer<*>,
    ) : AutoCloseable {

        val environment: Map<String, String> = mapOf(
            "SURF_REDIS_HOST" to redis.host,
            "SURF_REDIS_PORT" to redis.getMappedPort(REDIS_PORT).toString(),
            "SURF_NATS_SERVERS" to "nats://${nats.host}:${nats.getMappedPort(NATS_PORT)}",
        )

        override fun close() {
            nats.stop()
            redis.stop()
        }

        companion object {

            private const val REDIS_PORT = 6379
            private const val NATS_PORT = 4222

            /**
             * Starts both containers, or aborts when Docker is not usable.
             */
            fun start(): BrokerContainers {
                if (!DockerClientFactory.instance().isDockerAvailable) {
                    abort<Nothing>("Docker is not available, so Redis and NATS cannot be started.")
                }

                val redis = GenericContainer("redis:7")
                    .withExposedPorts(REDIS_PORT)
                    .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*", 1))
                val nats = GenericContainer("nats:2")
                    .withCommand("-js")
                    .withExposedPorts(NATS_PORT)
                    .waitingFor(Wait.forLogMessage(".*Server is ready.*", 1))

                try {
                    redis.start()
                    nats.start()
                } catch (failure: RuntimeException) {
                    nats.stop()
                    redis.stop()
                    abort<Nothing>("Redis and NATS could not be started in Docker: $failure")
                }

                return BrokerContainers(redis, nats)
            }
        }
    }

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

        for (jar in pluginsDir.listDirectoryEntries("*.jar")) {
            jar.deleteIfExists()
        }

        for (plugin in plugins) {
            plugin.copyTo(pluginsDir.resolve(plugin.name), overwrite = true)
        }

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

    /**
     * What the build handed over: where to work, which java to use, which extra JVM arguments the
     * server process takes, and what to fetch.
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
            jvmArgs = System.getProperty("surf.sharedBeanBootTest.jvmArgs").orEmpty()
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
                "./gradlew :surf-api-paper-shared-bean-test:paperSharedBeanBootTest",
        )

    private companion object {

        /**
         * Follows redirects, which both release hosts use.
         */
        val HTTP: HttpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build()

        val DOWNLOAD_TIMEOUT: Duration = Duration.ofMinutes(10)

        /**
         * Generous, because a first run downloads LuckPerms' own libraries as the server starts.
         */
        val BOOT_TIMEOUT: Duration = Duration.ofMinutes(5)

        /**
         * How far the log is allowed to lag the process.
         */
        val GRACE: Duration = Duration.ofSeconds(10)

        val POLL: Duration = Duration.ofMillis(250)

        val SHUTDOWN_TIMEOUT: Duration = Duration.ofSeconds(60)

        /**
         * Paper's own "the server is up" line, which the plugins' enable phase precedes.
         */
        const val DONE = "Done ("

        const val TAIL_LINES = 80

        const val RELEVANT_LINES = 40

        /**
         * What makes a log line worth quoting in a failure.
         *
         * The three plugin names and the two markers, so a reader can see which plugin contexts got
         * as far as running their beans; and the exception types a plugin context fails with when
         * its dependencies cannot be satisfied.
         */
        val INTERESTING = listOf(
            "surf-paper-api",
            "SurfSharedBeanProvider",
            "SurfSharedBeanConsumer",
            SharedBeanReport.MARKER,
            SharedGreetingImpl.PROVIDER_MARKER,
            "UnsatisfiedDependencyException",
            "PluginContextException",
        )

        /**
         * The fields of the consumer's report line as they read when every expectation holds.
         *
         * The provider's internals stay invisible, `Plugin` and the concrete plugin class both
         * resolve to the consumer itself, the context class loader is the plugin's during bean
         * creation, only the LuckPerms adapter exists because PlaceholderAPI is absent,
         * configuration classes get their CGLIB subclass, no context registered a JVM shutdown
         * hook, the root's `Server` is the one the plugin runs on, and the plugin's logger bean is
         * named like its own logger.
         */
        val EXPECTED_FIELDS = listOf(
            "sameLoader=true",
            "privateVisible=false",
            "pluginIsSelf=true",
            "ownPluginIsSelf=true",
            "pluginIdentity=true",
            "proxyEquals=true",
            "tccl=true",
            "probes=[LuckPermsProbe]",
            "configProxy=true",
            "luckPermsBean=true",
            "surfEvent=true",
            "shutdownHooks=0",
            "logImpl=SLF4JLocationAwareLog",
            "scopeSurvivesLoad=true",
            "surfConfig=configured",
            "redis=ok",
            "redisson=ok",
            "serverBean=true",
            "pluginLogger=true",
        )

        /**
         * The fields of the suspending command's line: its job descends from the consumer's own
         * coroutine scope, and it started on the server thread.
         */
        val EXPECTED_SCOPE_COMMAND_FIELDS = listOf(
            "commandScope=consumer",
            "commandThread=main",
        )

        /**
         * What the consumer's listener bean logs when the server finished starting.
         */
        val LISTENER_LINE = "${SharedBeanReport.SPIKE_MARKER} listener=serverLoad"

        /**
         * What the consumer's command bean logs when the console runs it.
         */
        val COMMAND_LINE = "${SharedBeanReport.SPIKE_MARKER} command=executed"

        /**
         * What the consumer's suspending command bean logs when the console runs it.
         */
        val SCOPE_COMMAND_LINE = "${SharedBeanReport.SPIKE_MARKER} commandScope="

        /**
         * What the consumer's NATS command bean logs when the console runs it, whatever the
         * outcome.
         */
        val NATS_COMMAND_LINE = "${SharedBeanReport.SPIKE_MARKER} nats="

        /**
         * The fields of the NATS command's line: a service call through a service proxy and an
         * event to the consumer's own listener both went through the broker and came back, and an
         * item stack survived the serializer round trip on the server.
         */
        val EXPECTED_NATS_COMMAND_FIELDS = listOf(
            "nats=ok",
            "natsEvent=ok",
            "natsItemStack=ok",
        )

        /**
         * The prefix of the line the consumer's disable hook logs for its coroutine join, whatever
         * the outcome.
         */
        val DISABLE_JOIN_MARKER = "${SharedBeanReport.SPIKE_MARKER} disableJoin="

        /**
         * The disable hook's line when its coroutine, suspended on server thread work, was
         * cancelled and joined while that thread was blocked in the hook.
         */
        val DISABLE_JOIN_LINE = "${DISABLE_JOIN_MARKER}returned"

        /**
         * Log lines that mean a surf plugin's context failed somewhere, even if the report
         * appeared.
         */
        val FAILURE_MARKERS = listOf("Failed to run bootstrapper", "PluginContextException")

        /**
         * A phase line of either test plugin; group 1 is the plugin role, group 2 the phase.
         */
        val PHASE_LINE = Regex(
            """${SharedBeanReport.SPIKE_MARKER} (provider|consumer) phase=(\w+)""",
        )

        /**
         * The phases in the order the design promises: bootstrap, load and enable go provider
         * first, disable goes consumer first, and the export is destroyed once, by its provider,
         * last.
         */
        val EXPECTED_PHASES = listOf(
            "provider:bootstrap", "consumer:bootstrap",
            "provider:load", "consumer:load",
            "provider:enable", "consumer:enable",
            "consumer:disable", "provider:disable",
            "provider:destroy",
        )

        /**
         * The ids of the three plugins surf-api's own `paper-plugin.yml` requires.
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
