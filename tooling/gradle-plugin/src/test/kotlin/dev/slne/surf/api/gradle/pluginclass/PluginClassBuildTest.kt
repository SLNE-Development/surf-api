package dev.slne.surf.api.gradle.pluginclass

import dev.slne.surf.microservice.gradle.testkit.TestBuild
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Builds with the surf plugins through TestKit, offline. The surf-api artifacts cannot be resolved, so
 * only tasks that need no dependency resolution run, and nothing is compiled.
 */
class PluginClassBuildTest {

    private fun project(info: TestInfo, plugin: String, extension: String, configure: String = ""): TestBuild =
        TestBuild.create(info.displayName, withSurfApi = true).apply {
            file("gradle.properties", "kotlin.stdlib.default.dependency=false")
            file(
                "build.gradle",
                """
                plugins { id '$plugin' }
                $extension {
                    $configure
                }
                """,
            )
        }

    private fun TestBuild.source(path: String, content: String) = file("src/main/$path", content)

    private fun TestBuild.pluginYml(): String =
        dir.resolve("build/generated/plugin-yml/Paper/paper-plugin.yml").readText()

    private fun TestBuild.baseSources() {
        source("kotlin/com/example/plugin/Greeter.kt", "package com.example.plugin\nclass Greeter")
        source("java/com/example/plugin/command/Command.java", "package com.example.plugin.command;\nclass Command {}")
    }

    @Test
    fun `paper plugin without a main class points at the generated classes`(info: TestInfo) {
        val build = project(info, PAPER_PLUGIN, "surfPaperPluginApi")
        build.baseSources()

        build.run("generatePaperPluginDescription", "generateSurfPluginClasses")

        val description = build.pluginYml()
        assertContains(description, "main: com.example.plugin.GeneratedSurfPlugin")
        assertContains(description, "bootstrapper: com.example.plugin.GeneratedSurfBootstrapper")

        val generated = build.dir.resolve("build/generated/surf-api/paper-plugin/com/example/plugin")
        assertContains(generated.resolve("GeneratedSurfPlugin.java").readText(), "extends SurfPaperPlugin")
        assertContains(generated.resolve("GeneratedSurfBootstrapper.java").readText(), "extends SurfPaperBootstrapper")
    }

    @Test
    fun `paper plugin keeps a main class and bootstrapper named in the build script`(info: TestInfo) {
        val build = project(
            info, PAPER_PLUGIN, "surfPaperPluginApi",
            "mainClass 'com.example.plugin.MyPlugin'\nbootstrapper 'com.example.plugin.MyBootstrapper'",
        )
        build.baseSources()

        build.run("generatePaperPluginDescription", "generateSurfPluginClasses")

        val description = build.pluginYml()
        assertContains(description, "main: com.example.plugin.MyPlugin")
        assertContains(description, "bootstrapper: com.example.plugin.MyBootstrapper")
        assertFalse(build.dir.resolve("build/generated/surf-api/paper-plugin/com").exists())
    }

    @Test
    fun `paper plugin generates only the class the build script does not name`(info: TestInfo) {
        val build = project(info, PAPER_PLUGIN, "surfPaperPluginApi", "mainClass 'com.example.plugin.MyPlugin'")
        build.baseSources()

        build.run("generatePaperPluginDescription", "generateSurfPluginClasses")

        assertContains(build.pluginYml(), "main: com.example.plugin.MyPlugin")
        assertContains(build.pluginYml(), "bootstrapper: com.example.plugin.GeneratedSurfBootstrapper")
        val generated = build.dir.resolve("build/generated/surf-api/paper-plugin/com/example/plugin")
        assertEquals(listOf("GeneratedSurfBootstrapper.java"), generated.list()!!.toList())
    }

    @Test
    fun `paper plugin lists surf plugins as server and bootstrap dependencies and CommandAPI as server dependency`(info: TestInfo) {
        val build = project(
            info, PAPER_PLUGIN, "surfPaperPluginApi",
            "surfPluginDependency 'surf-core-paper'\nsoftSurfPluginDependency 'surf-lobby-paper'",
        )
        build.baseSources()

        build.run("generatePaperPluginDescription", "generateSurfPluginClasses")

        val description = build.pluginYml()
        val (server, bootstrap) = description.substringAfter("dependencies:").let {
            it.substringAfter("server:").substringBefore("bootstrap:") to it.substringAfter("bootstrap:")
        }

        assertContains(server, "surf-core-paper:\n      load: BEFORE\n      required: true\n      join-classpath: true")
        assertContains(server, "surf-lobby-paper:\n      load: BEFORE\n      required: false\n      join-classpath: true")
        assertContains(server, "CommandAPI:\n      load: BEFORE\n      required: true\n      join-classpath: true")
        assertContains(bootstrap, "surf-core-paper:")
        assertContains(bootstrap, "surf-lobby-paper:")
        assertFalse("CommandAPI" in bootstrap, bootstrap)
    }

    @Test
    fun `the base package override moves the generated classes`(info: TestInfo) {
        val build = project(info, PAPER_PLUGIN, "surfPaperPluginApi", "basePackage = 'com.example'")
        build.baseSources()

        build.run("generatePaperPluginDescription", "generateSurfPluginClasses")

        assertContains(build.pluginYml(), "main: com.example.GeneratedSurfPlugin")
        assertEquals("com.example", build.dir.resolve("build/generated/surf-api/base-package/base-package.txt").readText())
    }

    @Test
    fun `sources without a common package fail the build with a hint`(info: TestInfo) {
        val build = project(info, PAPER_PLUGIN, "surfPaperPluginApi")
        build.source("kotlin/com/example/A.kt", "package com.example\nclass A")
        build.source("kotlin/org/example/B.kt", "package org.example\nclass B")

        val result = build.runAndFail("generatePaperPluginDescription")

        assertContains(result.output, "share no common package")
        assertContains(result.output, "basePackage")
    }

    @Test
    fun `a base package inside the surf namespace above surf-api fails the build`(info: TestInfo) {
        val build = project(info, PAPER_PLUGIN, "surfPaperPluginApi")
        build.source("kotlin/dev/slne/surf/Plugin.kt", "package dev.slne.surf\nclass Plugin")

        val result = build.runAndFail("generatePaperPluginDescription")

        assertContains(result.output, "contains surf-api's package")
    }

    @Test
    fun `a Paper plugin that names its classes needs no common package and is warned about the sources the scan misses`(info: TestInfo) {
        val build = project(
            info, PAPER_PLUGIN, "surfPaperPluginApi",
            "mainClass 'com.example.Main'\nbootstrapper 'com.example.Boot'\nbasePackage = 'dev.slne.surf.api'",
        )
        build.source("kotlin/com/example/Main.kt", "package com.example\nclass Main")
        build.source("kotlin/org/other/Other.kt", "package org.other\nclass Other")

        val result = build.run("generatePaperPluginDescription", "generateSurfPluginClasses")

        assertContains(build.pluginYml(), "main: com.example.Main")
        assertContains(build.pluginYml(), "bootstrapper: com.example.Boot")
        assertContains(result.output, "The package 'com.example' of the class 'com.example.Boot' does not contain the source packages 'org.other'")
        assertEquals("", build.dir.resolve("build/generated/surf-api/base-package/base-package.txt").readText())
    }

    @Test
    fun `a Paper plugin that generates only its main class still needs the base package`(info: TestInfo) {
        val build = project(info, PAPER_PLUGIN, "surfPaperPluginApi", "bootstrapper 'com.example.Boot'")
        build.source("kotlin/com/example/Main.kt", "package com.example\nclass Main")
        build.source("kotlin/org/other/Other.kt", "package org.other\nclass Other")

        val result = build.runAndFail("generatePaperPluginDescription")

        assertContains(result.output, "share no common package")
    }

    @Test
    fun `an explicit base package that misses a source package warns`(info: TestInfo) {
        val build = project(info, PAPER_PLUGIN, "surfPaperPluginApi", "basePackage = 'com.example.core'")
        build.source("kotlin/com/example/core/Core.kt", "package com.example.core\nclass Core")
        build.source("kotlin/com/example/listener/Listener.kt", "package com.example.listener\nclass Listener")

        val result = build.run("generatePaperPluginDescription", "generateSurfPluginClasses")

        assertContains(build.pluginYml(), "main: com.example.core.GeneratedSurfPlugin")
        assertContains(result.output, "The base package 'com.example.core' does not contain the source packages 'com.example.listener'")
    }

    @Test
    fun `a malformed explicit base package fails the build before the generator runs`(info: TestInfo) {
        val build = project(info, PAPER_PLUGIN, "surfPaperPluginApi", "basePackage = 'com.example.'")
        build.baseSources()

        val result = build.runAndFail("generateSurfPluginClasses")

        assertContains(result.output, "'com.example.' is not a valid package name")
    }

    @Test
    fun `a Paper plugin inside the surf-api namespace builds`(info: TestInfo) {
        val build = project(info, PAPER_PLUGIN, "surfPaperPluginApi")
        build.source("kotlin/dev/slne/surf/api/example/Example.kt", "package dev.slne.surf.api.example\nclass Example")

        build.run("generatePaperPluginDescription", "generateSurfPluginClasses")

        assertContains(build.pluginYml(), "main: dev.slne.surf.api.example.GeneratedSurfPlugin")
    }

    @Test
    fun `a Minestom plugin inside the surf-api namespace fails the build, generated or named`(info: TestInfo) {
        val generated = project(info, "dev.slne.surf.api.gradle.minestom", "surfMinestomApi")
        generated.source("kotlin/dev/slne/surf/api/example/Example.kt", "package dev.slne.surf.api.example\nclass Example")

        val generatedResult = generated.runAndFail("generateMinestomPluginFile")

        assertContains(generatedResult.output, "lies inside surf-api's package")

        val named = TestBuild.create("${info.displayName} named", withSurfApi = true)
        named.file("gradle.properties", "kotlin.stdlib.default.dependency=false")
        named.file(
            "build.gradle",
            """
            plugins { id 'dev.slne.surf.api.gradle.minestom' }
            minestomPluginFile { main = 'dev.slne.surf.api.example.Main' }
            """,
        )
        named.source("kotlin/dev/slne/surf/api/example/Example.kt", "package dev.slne.surf.api.example\nclass Example")

        val namedResult = named.runAndFail("computeSurfBasePackage")

        assertContains(namedResult.output, "lies inside surf-api's package")
    }

    @Test
    fun `a Velocity plugin that names its main class needs no common package`(info: TestInfo) {
        val build = TestBuild.create(info.displayName, withSurfApi = true)
        build.file("gradle.properties", "kotlin.stdlib.default.dependency=false")
        build.file(
            "build.gradle",
            """
            plugins { id 'dev.slne.surf.api.gradle.velocity' }
            velocityPluginFile { main = 'com.example.Main' }
            """,
        )
        build.source("kotlin/com/example/Main.kt", "package com.example\nclass Main")
        build.source("kotlin/org/other/Other.kt", "package org.other\nclass Other")

        val result = build.run("generateVelocityPluginFile", "generateSurfPluginClasses")

        val pluginFile = build.dir.resolve("build/generated/surf-api/velocity/velocity-plugin.json").readText()
        assertContains(pluginFile, "\"main\": \"com.example.Main\"")
        assertContains(result.output, "does not contain the source packages 'org.other'")
    }

    @Test
    fun `a packaging module without sources builds when it names its classes`(info: TestInfo) {
        val build = project(
            info, PAPER_PLUGIN, "surfPaperPluginApi",
            "mainClass 'com.example.Main'\nbootstrapper 'com.example.Boot'",
        )

        val result = build.run("generatePaperPluginDescription", "generateSurfPluginClasses")

        assertContains(build.pluginYml(), "main: com.example.Main")
        assertFalse("does not contain" in result.output, result.output)
    }

    @Test
    fun `generatePluginClass off writes no Velocity plugin file and generates no class`(info: TestInfo) {
        val build = project(info, "dev.slne.surf.api.gradle.velocity", "surfVelocityApi", "generatePluginClass = false")
        build.baseSources()

        build.run("generateVelocityPluginFile", "generateSurfPluginClasses")

        assertFalse(build.dir.resolve("build/generated/surf-api/velocity/velocity-plugin.json").exists())
        assertFalse(build.dir.resolve("build/generated/surf-api/velocity-plugin/com").exists())
    }

    @Test
    fun `generatePluginClass off still writes the Velocity plugin file for a named main class`(info: TestInfo) {
        val build = TestBuild.create(info.displayName, withSurfApi = true)
        build.file("gradle.properties", "kotlin.stdlib.default.dependency=false")
        build.file(
            "build.gradle",
            """
            plugins { id 'dev.slne.surf.api.gradle.velocity' }
            surfVelocityApi { generatePluginClass = false }
            velocityPluginFile { main = 'com.example.plugin.MyPlugin' }
            """,
        )
        build.baseSources()

        build.run("generateVelocityPluginFile", "generateSurfPluginClasses")

        val pluginFile = build.dir.resolve("build/generated/surf-api/velocity/velocity-plugin.json").readText()
        assertContains(pluginFile, "\"main\": \"com.example.plugin.MyPlugin\"")
    }

    @Test
    fun `generatePluginClass off writes no Minestom plugin file, service entry or class path copy`(info: TestInfo) {
        val build = project(info, "dev.slne.surf.api.gradle.minestom", "surfMinestomApi", "generatePluginClass = false")
        build.baseSources()

        build.run(
            "generateMinestomPluginFile", "generateMinestomPluginIndex",
            "generateMinestomPluginClassPathFile", "generateSurfPluginClasses",
        )

        val generated = build.dir.resolve("build/generated/surf-api")
        assertFalse(generated.resolve("minestom/minestom-plugin.json").exists())
        assertTrue(generated.resolve("minestom-classpath").walkTopDown().none { it.isFile })
        assertFalse(generated.resolve("minestom-plugin/com").exists())
    }

    @Test
    fun `generatePluginClass off keeps the Minestom files of a named main class and drops them when it goes`(info: TestInfo) {
        val build = TestBuild.create(info.displayName, withSurfApi = true)
        build.file("gradle.properties", "kotlin.stdlib.default.dependency=false")
        fun script(main: String) = build.file(
            "build.gradle",
            """
            plugins { id 'dev.slne.surf.api.gradle.minestom' }
            surfMinestomApi { generatePluginClass = false }
            $main
            """,
        )
        script("minestomPluginFile { main = 'com.example.plugin.MyPlugin' }")
        build.baseSources()
        val tasks = arrayOf(
            "generateMinestomPluginFile", "generateMinestomPluginIndex", "generateMinestomPluginClassPathFile",
        )

        build.run(*tasks)

        val generated = build.dir.resolve("build/generated/surf-api")
        val main = "com.example.plugin.MyPlugin"
        assertContains(generated.resolve("minestom/minestom-plugin.json").readText(), "\"main\": \"$main\"")
        val index = "META-INF/services/dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin"
        assertEquals(main, generated.resolve("minestom-classpath/index/$index").readText())
        assertTrue(generated.resolve("minestom-classpath/descriptor/META-INF/surf-minestom-plugins/$main.json").isFile)

        script("")
        build.run(*tasks)

        assertFalse(generated.resolve("minestom/minestom-plugin.json").exists())
        assertTrue(generated.resolve("minestom-classpath").walkTopDown().none { it.isFile })
    }

    @Test
    fun `the base package is cached until a source changes`(info: TestInfo) {
        val build = project(info, PAPER_PLUGIN, "surfPaperPluginApi")
        build.baseSources()

        assertEquals(TaskOutcome.SUCCESS, build.run("computeSurfBasePackage").task(":computeSurfBasePackage")?.outcome)
        assertEquals(TaskOutcome.UP_TO_DATE, build.run("computeSurfBasePackage").task(":computeSurfBasePackage")?.outcome)

        build.source("kotlin/org/other/Other.kt", "package org.other\nclass Other")
        val result = build.runAndFail("computeSurfBasePackage")

        assertContains(result.output, "share no common package")
    }

    @Test
    fun `velocity plugin file points at the generated main class`(info: TestInfo) {
        val build = project(info, "dev.slne.surf.api.gradle.velocity", "surfVelocityApi")
        build.baseSources()

        build.run("generateVelocityPluginFile", "generateSurfPluginClasses")

        val pluginFile = build.dir.resolve("build/generated/surf-api/velocity/velocity-plugin.json").readText()
        assertContains(pluginFile, "\"main\": \"com.example.plugin.GeneratedSurfPlugin\"")
        val generated = build.dir.resolve("build/generated/surf-api/velocity-plugin/com/example/plugin/GeneratedSurfPlugin.java")
        assertContains(generated.readText(), "extends SurfVelocityPlugin")
    }

    @Test
    fun `velocity plugin file keeps a main class named in the build script`(info: TestInfo) {
        val build = project(
            info, "dev.slne.surf.api.gradle.velocity", "surfVelocityApi",
        )
        build.file(
            "build.gradle",
            """
            plugins { id 'dev.slne.surf.api.gradle.velocity' }
            velocityPluginFile { main = 'com.example.plugin.MyPlugin' }
            """,
        )
        build.baseSources()

        build.run("generateVelocityPluginFile", "generateSurfPluginClasses")

        val pluginFile = build.dir.resolve("build/generated/surf-api/velocity/velocity-plugin.json").readText()
        assertContains(pluginFile, "\"main\": \"com.example.plugin.MyPlugin\"")
        assertFalse(build.dir.resolve("build/generated/surf-api/velocity-plugin/com").exists())
    }

    @Test
    fun `minestom plugin file, index and class path copy point at the generated main class`(info: TestInfo) {
        val build = project(info, "dev.slne.surf.api.gradle.minestom", "surfMinestomApi")
        build.baseSources()

        build.run(
            "generateMinestomPluginFile", "generateMinestomPluginIndex",
            "generateMinestomPluginClassPathFile", "generateSurfPluginClasses",
        )

        val main = "com.example.plugin.GeneratedSurfPlugin"
        val generated = build.dir.resolve("build/generated/surf-api")
        assertContains(generated.resolve("minestom/minestom-plugin.json").readText(), "\"main\": \"$main\"")
        assertEquals(
            main,
            generated.resolve("minestom-classpath/index/META-INF/services/dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin").readText(),
        )
        assertContains(
            generated.resolve("minestom-classpath/descriptor/META-INF/surf-minestom-plugins/$main.json").readText(),
            "\"main\": \"$main\"",
        )
        assertContains(
            generated.resolve("minestom-plugin/com/example/plugin/GeneratedSurfPlugin.java").readText(),
            "extends SurfMinestomPlugin",
        )
    }

    @Test
    fun `the base package and class generation tasks work with the configuration cache`(info: TestInfo) {
        val build = project(info, PAPER_PLUGIN, "surfPaperPluginApi")
        build.baseSources()
        val tasks = arrayOf("computeSurfBasePackage", "generateSurfPluginClasses", "--configuration-cache")

        val stored = build.run(*tasks)
        val reused = build.run(*tasks)

        assertContains(stored.output, "Configuration cache entry stored")
        assertContains(reused.output, "Configuration cache entry reused")
        assertEquals("com.example.plugin", build.dir.resolve("build/generated/surf-api/base-package/base-package.txt").readText())
    }

    @Test
    fun `the Spring check works with the configuration cache`(info: TestInfo) {
        val build = springProject(info, extension = "")
        val tasks = arrayOf("checkNoBundledSpring", "--configuration-cache")

        val stored = build.runAndFail(*tasks)
        val reused = build.runAndFail(*tasks)

        assertContains(stored.output, "org.springframework:spring-core:1.0")
        assertContains(reused.output, "Reusing configuration cache")
    }

    @Test
    fun `the Spring check fails a build that shades Spring`(info: TestInfo) {
        val build = springProject(info, extension = "")

        val result = build.runAndFail("checkNoBundledSpring")

        assertContains(result.output, "org.springframework:spring-core:1.0")
        assertContains(result.output, "allowBundledSpring")
    }

    @Test
    fun `the Spring check passes when the build allows Spring`(info: TestInfo) {
        val build = springProject(info, extension = "allowBundledSpring = true")

        val result = build.run("checkNoBundledSpring")

        assertEquals(TaskOutcome.SKIPPED, result.task(":checkNoBundledSpring")?.outcome)
    }

    @Test
    fun `the Spring check passes without Spring in the runtime classpath`(info: TestInfo) {
        val build = springProject(info, extension = "", dependency = "")

        val result = build.run("checkNoBundledSpring")

        assertEquals(TaskOutcome.SUCCESS, result.task(":checkNoBundledSpring")?.outcome)
    }

    @Test
    fun `a passed Spring check is up to date until the bundled modules change`(info: TestInfo) {
        val build = springProject(info, extension = "", dependency = "")

        assertEquals(TaskOutcome.SUCCESS, build.run("checkNoBundledSpring").task(":checkNoBundledSpring")?.outcome)
        assertEquals(TaskOutcome.UP_TO_DATE, build.run("checkNoBundledSpring").task(":checkNoBundledSpring")?.outcome)
    }

    @Test
    fun `the Spring check ignores a Spring BOM that contributes no artifact`(info: TestInfo) {
        val build = springProject(info, extension = "", dependency = "implementation platform('org.springframework:spring-framework-bom:1.0')")
        publishPom(build.dir.resolve("local-repository"), "org.springframework", "spring-framework-bom", "1.0", "pom")

        val result = build.run("checkNoBundledSpring")

        assertEquals(TaskOutcome.SUCCESS, result.task(":checkNoBundledSpring")?.outcome)
    }

    @Test
    fun `the Spring check fails a build that shades a jar carrying Spring under another coordinate`(info: TestInfo) {
        val build = springProject(info, extension = "", dependency = "implementation 'com.example:fat:1.0'")

        val result = build.runAndFail("checkNoBundledSpring")

        assertContains(result.output, "org/springframework classes in fat-1.0.jar")
        assertContains(result.output, "allowBundledSpring")
    }

    /**
     * A velocity plugin whose runtime classpath holds a Spring stub from a local Maven repository.
     *
     * The repository also holds `com.example:fat:1.0`, whose jar carries an unrelocated Spring class.
     */
    private fun springProject(
        info: TestInfo,
        extension: String,
        dependency: String = "implementation 'org.springframework:spring-core:1.0'",
    ): TestBuild {
        val build = TestBuild.create(info.displayName, withSurfApi = true)
        build.file("gradle.properties", "kotlin.stdlib.default.dependency=false")
        build.baseSources()
        val repository = build.dir.resolve("local-repository")
        publishStub(repository, "org.springframework", "spring-core", "1.0")
        publishStub(repository, "com.example", "fat", "1.0", "org/springframework/core/SpringVersion.class")
        build.file(
            "build.gradle",
            """
            plugins { id 'dev.slne.surf.api.gradle.velocity' }
            surfVelocityApi { $extension }
            repositories { maven { url = uri('${repository.invariantSeparatorsPath}') } }
            dependencies { $dependency }
            """,
        )
        return build
    }

    /**
     * Publishes a POM without a jar, as a BOM is.
     */
    private fun publishPom(repository: File, group: String, name: String, version: String, packaging: String): File {
        val directory = repository.resolve("${group.replace('.', '/')}/$name/$version").apply { mkdirs() }
        directory.resolve("$name-$version.pom").writeText(
            """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>$group</groupId>
              <artifactId>$name</artifactId>
              <version>$version</version>
              <packaging>$packaging</packaging>
            </project>
            """.trimIndent()
        )
        return directory
    }

    private fun publishStub(repository: File, group: String, name: String, version: String, entry: String = "stub.txt") {
        val directory = publishPom(repository, group, name, version, "jar")
        JarOutputStream(directory.resolve("$name-$version.jar").outputStream()).use { jar ->
            jar.putNextEntry(JarEntry(entry))
            jar.write(0)
            jar.closeEntry()
        }
    }

    private companion object {
        const val PAPER_PLUGIN = "dev.slne.surf.api.gradle.paper-plugin"
    }
}
