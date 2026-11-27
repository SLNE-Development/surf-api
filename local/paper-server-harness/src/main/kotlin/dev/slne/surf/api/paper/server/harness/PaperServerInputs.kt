package dev.slne.surf.api.paper.server.harness

import org.junit.jupiter.api.Assumptions.abort
import java.io.File
import java.io.IOException
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
import kotlin.io.path.*

/**
 * What the build hands a Paper server test: where to work, which java to run the server with,
 * which extra JVM arguments the server takes, what to download and which plugin jars to install.
 *
 * Every missing or unobtainable input aborts the test instead of failing it. A missing network, a
 * moved release or a changed checksum is not a defect in this repository, and a test that goes red
 * for one of those teaches people to ignore it.
 *
 * @property serverJarOverride a server jar that replaces the pinned Paper jar, or `null` to run
 *   the pinned one
 */
class PaperServerInputs(
    val workDir: Path,
    val downloadsDir: Path,
    val javaLauncher: Path,
    val jvmArgs: List<String>,
    private val serverJarOverride: Path?,
    private val values: Properties,
) {

    /**
     * The server jar to run: the one the build was given, or else the pinned Paper jar.
     */
    fun serverJar(): Path {
        if (serverJarOverride == null) return download(PAPER)

        if (!serverJarOverride.isRegularFile()) {
            abort<Nothing>("The server jar $serverJarOverride the build was given does not exist.")
        }

        return serverJarOverride
    }

    /**
     * The pinned plugins surf-api's own `paper-plugin.yml` requires.
     */
    fun thirdPartyPlugins(): List<Path> = THIRD_PARTY_PLUGINS.map(::download)

    /**
     * The plugin jars built from this repository, as the build resolved them.
     *
     * A missing one aborts: the build did not produce what the test was to run, and the build has
     * already said why.
     */
    fun pluginJars(): List<Path> {
        val declared = (values.getProperty("pluginJars") ?: "")
            .split(File.pathSeparatorChar)
            .filter { it.isNotBlank() }
            .map { Path.of(it) }

        if (declared.isEmpty()) {
            abort<Nothing>("The build resolved no plugin jars for the test to install.")
        }

        val missing = declared.filterNot { it.isRegularFile() }

        if (missing.isNotEmpty()) {
            abort<Nothing>(
                "These plugin jars the build named do not exist: " +
                    missing.joinToString { it.absolutePathString() },
            )
        }

        return declared
    }

    /**
     * [id]'s pinned artifact in [downloadsDir], fetched unless a byte-identical copy is already
     * there.
     *
     * The checksum makes the cache safe as well as the download pinned: a half-written file from an
     * interrupted run is replaced instead of reused, and an artifact whose host changed what it
     * serves never reaches the server.
     */
    fun download(id: String): Path {
        val fileName = property("download.$id.file")
        val url = property("download.$id.url")
        val expected = property("download.$id.sha256").lowercase()
        val target = downloadsDir.createDirectories().resolve(fileName)

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

    private fun property(key: String): String {
        return values.getProperty(key) ?: abort<Nothing>("The build's input file has no '$key'.")
    }

    companion object {

        private const val PAPER = "paper"

        /**
         * The ids of the three plugins surf-api's own `paper-plugin.yml` requires.
         */
        private val THIRD_PARTY_PLUGINS = listOf("commandapi", "luckperms", "packetevents")

        /**
         * Follows redirects, which both release hosts use.
         */
        private val HTTP: HttpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build()

        private val DOWNLOAD_TIMEOUT: Duration = Duration.ofMinutes(10)

        /**
         * Reads the system properties the build's `registerPaperServerTest` task sets, or aborts
         * when the test was not started by such a task.
         */
        fun fromSystemProperties(): PaperServerInputs {
            val inputsFile = systemPath("surf.paperServerTest.inputs")
            val javaLauncher = systemPath("surf.paperServerTest.java")

            if (!inputsFile.isRegularFile()) {
                abort<Nothing>("The build's input file $inputsFile does not exist.")
            }

            if (!javaLauncher.isRegularFile()) {
                abort<Nothing>("The java launcher $javaLauncher the build named does not exist.")
            }

            val values = Properties()
            Files.newBufferedReader(inputsFile).use { values.load(it) }

            return PaperServerInputs(
                workDir = systemPath("surf.paperServerTest.workDir").createDirectories(),
                downloadsDir = systemPath("surf.paperServerTest.downloads"),
                javaLauncher = javaLauncher,
                jvmArgs = System.getProperty("surf.paperServerTest.jvmArgs").orEmpty()
                    .split(Regex("\\s+"))
                    .filter { it.isNotEmpty() },
                serverJarOverride = System.getProperty("surf.paperServerTest.serverJar")
                    ?.let(Path::of),
                values = values,
            )
        }

        private fun systemPath(key: String): Path {
            val value = System.getProperty(key)
                ?: abort<Nothing>(
                    "-D$key is not set, so this test was not started by a Gradle task that " +
                        "registerPaperServerTest registered.",
                )

            return Path.of(value)
        }

        private fun sha256(file: Path): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(file.readBytes())

            return HexFormat.of().formatHex(digest)
        }
    }
}
