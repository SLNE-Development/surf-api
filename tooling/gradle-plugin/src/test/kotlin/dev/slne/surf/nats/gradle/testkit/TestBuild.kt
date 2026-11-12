package dev.slne.surf.nats.gradle.testkit

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

/**
 * A throw-away Gradle build in `build/tmp/testkit`, run through Gradle TestKit with the plugin under test.
 *
 * Builds use the Groovy DSL and never apply the Kotlin plugin, so the plugin is exercised without KGP and
 * surf-api. The environment is stripped of `WEAVER_URL` and `WEAVER_TOKEN` unless a test passes them explicitly.
 */
internal class TestBuild(val dir: File) {

    fun file(path: String, content: String): File {
        val file = dir.resolve(path)
        file.parentFile.mkdirs()
        file.writeText(content.trimIndent() + "\n", Charsets.UTF_8)
        return file
    }

    fun settings(vararg projects: String, rootName: String = "demo"): File {
        val includes = projects.joinToString("") { "include '$it'\n" }
        return file("settings.gradle", "rootProject.name = '$rootName'\n$includes")
    }

    fun script(content: String, project: String? = null): File =
        file(if (project == null) "build.gradle" else "$project/build.gradle", content)

    fun proto(path: String, content: String, project: String? = null): File =
        file(listOfNotNull(project, "src/main/proto", path).joinToString("/"), content)

    fun resolve(path: String): File = dir.resolve(path)

    fun run(vararg arguments: String, environment: Map<String, String> = emptyMap()): BuildResult =
        runner(arguments, environment).build()

    fun runAndFail(vararg arguments: String, environment: Map<String, String> = emptyMap()): BuildResult =
        runner(arguments, environment).buildAndFail()

    /**
     * Returns the task paths of a `--dry-run` of [tasks] in execution order.
     */
    fun plan(vararg tasks: String, environment: Map<String, String> = emptyMap()): List<String> =
        run("--dry-run", *tasks, environment = environment).plan()

    private fun runner(arguments: Array<out String>, environment: Map<String, String>): GradleRunner =
        GradleRunner.create()
            .withProjectDir(dir)
            .withPluginClasspath()
            .withTestKitDir(TESTKIT_HOME)
            .withEnvironment(baseEnvironment() + environment)
            .withArguments(arguments.toList() + "--no-build-cache")

    private fun baseEnvironment(): Map<String, String> =
        System.getenv().filterKeys { key -> LEAKING_VARIABLES.none { it.equals(key, ignoreCase = true) } }

    companion object {
        const val PLUGIN = "plugins { id 'dev.slne.surf.api.gradle.nats' }"

        /**
         * `maven-publish` with a file repository, which makes the project a published module.
         */
        const val PUBLISHING = """
            apply plugin: 'maven-publish'
            publishing {
                repositories { maven { name = 'local'; url = uri('build/repo') } }
                publications { m(MavenPublication) { } }
            }
        """

        const val CONTRACT_PATH = "surf/demo/v1/contract.proto"

        private val TESTKIT_HOME = File("build/tmp/testKit").absoluteFile
        private val LEAKING_VARIABLES = listOf("WEAVER_URL", "WEAVER_TOKEN")

        /**
         * Returns a Groovy `show` task that prints each expression as `[show] key=value`.
         */
        fun showTask(vararg values: Pair<String, String>): String = buildString {
            appendLine("tasks.register('show') { doLast {")
            values.forEach { (key, expression) -> appendLine("    println '[show] $key=' + ($expression)") }
            appendLine("} }")
        }

        fun contract(body: String, imports: String = "", pkg: String = "surf.demo.v1"): String = """
            syntax = "proto2";
            package $pkg;
            $imports
            ${body.trimIndent()}
        """.trimIndent()

        fun create(name: String): TestBuild {
            val root = File("build/tmp/testkit").absoluteFile
            val slug = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(36)
            var dir = root.resolve("%08x-%s".format(name.hashCode(), slug))
            if (dir.exists() && !dir.deleteRecursively()) dir = File("$dir-${System.nanoTime()}")
            dir.mkdirs()
            return TestBuild(dir)
        }
    }
}
