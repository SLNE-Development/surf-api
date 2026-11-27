import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import java.io.File

/**
 * The source set of a Paper server test, which no lifecycle task reaches.
 */
const val PAPER_SERVER_TEST_SOURCE_SET = "paperTest"

/**
 * The configuration holding the plugin jars a Paper server test installs next to the pinned
 * third-party plugins.
 */
const val PAPER_SERVER_PLUGINS = "serverPlugins"

/**
 * The task that writes [paperServerDownloads] and the plugin jars to the test's input file.
 */
const val WRITE_PAPER_SERVER_TEST_INPUTS = "writePaperServerTestInputs"

/**
 * One artifact a Paper server test downloads: its name on disk, where it comes from and what it
 * has to be.
 */
data class PinnedDownload(
    val id: String,
    val fileName: String,
    val url: String,
    val sha256: String,
) {

    /**
     * The three lines describing this artifact in the test's input file.
     */
    fun asProperties(): String {
        return """
            download.$id.file=$fileName
            download.$id.url=$url
            download.$id.sha256=$sha256
        """.trimIndent()
    }
}

/**
 * Pins every artifact a Paper server test downloads, with the checksum of that exact version.
 *
 * The three plugin versions come from `gradle/libs.versions.toml`, so the tests run against the
 * versions the repository declares. None of them is obtainable as a usable Maven artifact:
 * `dev.jorel:commandapi-paper-plugin` stops at 12.0.0 on Maven Central, and that build refuses to
 * start on Paper 26.3; `com.github.retrooper:packetevents-spigot` is published, but as a thin jar
 * whose manifest `Class-Path` names sibling jars rather than as the shaded plugin a server loads;
 * and LuckPerms publishes only its api to Maven. Each one is therefore a pinned URL plus the
 * checksum of that version, and bumping a version means updating the checksum with it. A mismatch
 * makes the tests skip and say so rather than run against something unknown.
 */
fun paperServerDownloads(libs: LibrariesForLibs): List<PinnedDownload> {
    val commandApi = libs.versions.commandapiplugin.get()
    val packetEvents = libs.versions.packetevents.plugin.get()
    val luckPerms = modrinthRelease(libs.versions.luckpermsplugin.bukkit.get())

    return listOf(
        // Paper 26.3 build 245. The download path is keyed by the jar's own sha256; the build index
        // is https://fill.papermc.io/v3/projects/paper/versions/26.3/builds?channel=STABLE.
        PinnedDownload(
            id = "paper",
            fileName = "paper-26.3-245.jar",
            url = "https://fill-data.papermc.io/v1/objects/" +
                "192d277ea1ce29b2df3933dd488521d4555cc82be864ea8df811fe536e8aed43/" +
                "paper-26.3-245.jar",
            sha256 = "192d277ea1ce29b2df3933dd488521d4555cc82be864ea8df811fe536e8aed43",
        ),
        PinnedDownload(
            id = "commandapi",
            fileName = "CommandAPI-$commandApi-Paper.jar",
            url = "https://github.com/CommandAPI/CommandAPI/releases/download/" +
                "$commandApi/CommandAPI-$commandApi-Paper.jar",
            sha256 = "ab48e55fd06264dd239cf015b501936cb50eb6c38d967fbffca6c91a48064049",
        ),
        PinnedDownload(
            id = "packetevents",
            fileName = "packetevents-spigot-$packetEvents.jar",
            url = "https://github.com/retrooper/packetevents/releases/download/" +
                "v$packetEvents/packetevents-spigot-$packetEvents.jar",
            sha256 = "060087c58ec268eae7dd0eb1f17ac3bea1eaadda1693414141ff64e506091bd7",
        ),
        // LuckPerms' Bukkit plugin jar, from Modrinth's permanent per-version CDN path. LuckPerms
        // downloads its own libraries into `plugins/LuckPerms/libs` on its first start, which is
        // why an assembled server directory is kept between runs.
        PinnedDownload(
            id = "luckperms",
            fileName = "LuckPerms-Bukkit-$luckPerms.jar",
            url = "https://cdn.modrinth.com/data/Vebnzrzj/versions/b0mk8uS6/" +
                "LuckPerms-Bukkit-$luckPerms.jar",
            sha256 = "49cecb66fa1fd22a133039a490e9c1e5095a238e7cd66eb9d2a16fe6c897550d",
        ),
    )
}

/**
 * The release part of a Modrinth version name such as `v5.5.71-bukkit`.
 */
private fun modrinthRelease(version: String): String {
    return version.removePrefix("v").substringBefore('-')
}

/**
 * Registers a [Test] task that runs the `paperTest` source set against a real Paper server.
 *
 * The project must apply the `paper-server-test` convention. The task hands the test the input
 * file, the work and download directories and the java launcher as system properties, and it is
 * never up to date. Two Gradle properties reach it:
 *
 * - `surf.bootTest.jvmArgs`: whitespace-separated JVM arguments for the test JVM, which downloads,
 *   and for the server process, for example `-Djavax.net.ssl.trustStoreType=Windows-ROOT` behind
 *   a TLS-intercepting proxy
 * - `surf.paperServerTest.serverJar`: a server jar that replaces the pinned Paper jar, such as a
 *   locally built SurfCanvas jar
 */
fun Project.registerPaperServerTest(name: String, description: String): TaskProvider<Test> {
    val sourceSet = the<SourceSetContainer>().getByName(PAPER_SERVER_TEST_SOURCE_SET)
    val writeInputs = tasks.named(WRITE_PAPER_SERVER_TEST_INPUTS)
    val jvmArguments = providers.gradleProperty("surf.bootTest.jvmArgs")
        .map { it.trim().split(Regex("\\s+")).filter(String::isNotEmpty) }
        .orElse(emptyList())
    val serverJar = providers.gradleProperty("surf.paperServerTest.serverJar")
        .map { file(it).absolutePath }
    val javaLauncher = the<JavaToolchainService>()
        .launcherFor(the<JavaPluginExtension>().toolchain)
    val workDir = paperServerTestWorkDir()
    val downloadsDir = rootProject.layout.buildDirectory.dir("paper-server-downloads")

    return tasks.register<Test>(name) {
        group = "verification"
        this.description = description

        testClassesDirs = sourceSet.output.classesDirs
        classpath = sourceSet.runtimeClasspath
        useJUnitPlatform()

        dependsOn(writeInputs)

        jvmArgs(jvmArguments.get())
        systemProperty("surf.paperServerTest.jvmArgs", jvmArguments.get().joinToString(" "))
        systemProperty(
            "surf.paperServerTest.inputs",
            workDir.resolve(PAPER_SERVER_TEST_INPUTS_FILE).absolutePath,
        )
        systemProperty("surf.paperServerTest.workDir", workDir.absolutePath)
        systemProperty("surf.paperServerTest.downloads", downloadsDir.get().asFile.absolutePath)
        systemProperty(
            "surf.paperServerTest.java",
            javaLauncher.get().executablePath.asFile.absolutePath,
        )

        if (serverJar.isPresent) {
            systemProperty("surf.paperServerTest.serverJar", serverJar.get())
        }

        // Booting a server is never up to date in any sense a person asking for this task means.
        outputs.upToDateWhen { false }

        testLogging {
            showStandardStreams = true
            events("passed", "failed", "skipped")
        }
    }
}

/**
 * The file name of the input file below [paperServerTestWorkDir].
 */
internal const val PAPER_SERVER_TEST_INPUTS_FILE = "inputs.properties"

/**
 * Where a Paper server test assembles its server.
 *
 * Under the build directory, so `clean` discards it while a second run reuses Paper's extracted
 * libraries and the libraries LuckPerms fetches on its first start.
 */
internal fun Project.paperServerTestWorkDir(): File {
    return layout.buildDirectory.dir("paper-server-test").get().asFile
}
