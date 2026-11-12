package dev.slne.surf.microservice.gradle.testkit

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.util.*

/**
 * A throw-away Groovy DSL build in `build/tmp/testkit`, run offline through Gradle TestKit.
 *
 * [withSurfApi] decides whether surf-api's Gradle plugins (and with them Shadow, KSP and Kotlin) are on the
 * plugin classpath next to the plugin under test.
 */
internal class TestBuild private constructor(val dir: File, private val withSurfApi: Boolean) {

    fun file(path: String, content: String): File {
        val file = dir.resolve(path)
        file.parentFile.mkdirs()
        file.writeText(content.trimIndent() + "\n", Charsets.UTF_8)
        return file
    }

    fun script(content: String): File = file("build.gradle", content)

    fun run(vararg arguments: String): BuildResult = runner(arguments).build()

    fun runAndFail(vararg arguments: String): BuildResult = runner(arguments).buildAndFail()

    private fun runner(arguments: Array<out String>): GradleRunner =
        GradleRunner.create()
            .withProjectDir(dir)
            .withPluginClasspath(if (withSurfApi) surfApiClasspath else pluginClasspath)
            .withTestKitDir(TESTKIT_HOME)
            .withArguments(arguments.toList() + listOf("--offline", "--stacktrace"))

    companion object {
        private val TESTKIT_HOME = File("build/tmp/testKit").absoluteFile

        private val pluginClasspath: List<File> by lazy {
            val metadata = Properties()
            val resource = checkNotNull(TestBuild::class.java.getResourceAsStream("/plugin-under-test-metadata.properties")) {
                "plugin-under-test-metadata.properties is missing from the test classpath"
            }
            resource.use { stream -> metadata.load(stream) }
            metadata.getProperty("implementation-classpath").toFiles()
        }

        private val surfApiClasspath: List<File> by lazy {
            val surfApi = checkNotNull(System.getProperty("surf.microservice.test.surfApiClasspath")) {
                "System property surf.microservice.test.surfApiClasspath is not set; run the tests through Gradle"
            }
            pluginClasspath + surfApi.toFiles()
        }

        private fun String.toFiles(): List<File> = split(File.pathSeparatorChar).filter { it.isNotBlank() }.map(::File)

        fun create(name: String, withSurfApi: Boolean): TestBuild {
            val slug = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(48)
            val dir = File("build/tmp/testkit-projects").absoluteFile.resolve(slug)
            dir.deleteRecursively()
            dir.mkdirs()
            return TestBuild(dir, withSurfApi).apply {
                file("settings.gradle", "rootProject.name = 'demo'")
            }
        }
    }
}
