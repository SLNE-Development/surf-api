package dev.slne.surf.api.gradle.testing

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SurfTestingConventionsFunctionalTest {
    @TempDir
    lateinit var projectDir: Path

    @Test
    fun `supplied libraries compile and execute tests and benchmarks`() {
        projectDir.writeFile(
            "settings.gradle.kts",
            """
                rootProject.name = "surf-testing-functional-test"
            """.trimIndent()
        )
        projectDir.writeFile(
            "build.gradle.kts",
            """
                plugins {
                    id("dev.slne.surf.api.gradle.core")
                }

                surfCoreApi {
                    addSurfApiToClasspath(false)
                }

                dependencies {
                    compileOnly("com.google.guava:guava:33.6.0-jre")
                }

                publishing {
                    publications {
                        create<MavenPublication>("surf") {
                            from(components["java"])
                        }
                    }
                }

                tasks.register("verifySurfTestingConventions") {
                    dependsOn(
                        "shadowJar",
                        "generatePomFileForSurfPublication",
                        "generateMetadataFileForSurfPublication"
                    )

                    doLast {
                        val compileOnly = configurations.getByName("compileOnly")
                        check(configurations.getByName("testCompileOnly").extendsFrom.contains(compileOnly))
                        check(configurations.getByName("testRuntimeOnly").extendsFrom.contains(compileOnly))
                        check(configurations.getByName("benchmarkCompileOnly").extendsFrom.contains(compileOnly))
                        check(configurations.getByName("benchmarkRuntimeOnly").extendsFrom.contains(compileOnly))

                        val testDependencies = configurations.getByName("testImplementation")
                            .allDependencies
                            .map { "${'$'}{it.group}:${'$'}{it.name}" }
                            .toSet()
                        check("org.jetbrains.kotlin:kotlin-test-junit5" in testDependencies)
                        check("org.junit.jupiter:junit-jupiter" in testDependencies)
                        check("org.jetbrains.kotlinx:kotlinx-coroutines-test" in testDependencies)
                        check("io.mockk:mockk" in testDependencies)
                        check("org.jetbrains.lincheck:lincheck" in testDependencies)

                        val benchmarkDependencies = configurations.getByName("benchmarkImplementation")
                            .allDependencies
                            .map { "${'$'}{it.group}:${'$'}{it.name}" }
                            .toSet()
                        check("org.jetbrains.kotlinx:kotlinx-benchmark-runtime" in benchmarkDependencies)

                        check(sourceSets.findByName("benchmark") != null)
                        check(tasks.findByName("benchmark") != null)
                        check(tasks.findByName("benchmarkBenchmark") != null)

                        val checkTask = tasks.getByName("check")
                        val checkDependencies = checkTask.taskDependencies.getDependencies(checkTask)
                        check(checkDependencies.none { it.name.contains("benchmark", ignoreCase = true) })

                        val forbiddenGroups = setOf(
                            "org.junit.jupiter",
                            "org.junit.platform",
                            "io.mockk",
                            "org.jetbrains.lincheck",
                            "org.jetbrains.kotlinx"
                        )
                        listOf("api", "implementation", "compileOnly", "runtimeOnly").forEach { name ->
                            check(configurations.getByName(name).allDependencies.none {
                                it.group in forbiddenGroups && it.name != "kotlinx-coroutines-core"
                            })
                        }

                        val forbiddenPublishedCoordinates = setOf(
                            "org.junit.jupiter",
                            "org.junit.platform",
                            "kotlin-test-junit5",
                            "kotlinx-coroutines-test",
                            "io.mockk",
                            "org.jetbrains.lincheck",
                            "kotlinx-benchmark-runtime"
                        )
                        val publishedMetadata = listOf(
                            layout.buildDirectory.file("publications/surf/pom-default.xml").get().asFile,
                            layout.buildDirectory.file("publications/surf/module.json").get().asFile
                        ).joinToString("\n") { it.readText() }
                        check(forbiddenPublishedCoordinates.none(publishedMetadata::contains))

                        val shadedJar = tasks.getByName("shadowJar").outputs.files.singleFile
                        val forbiddenShadedClasses = zipTree(shadedJar).matching {
                            include(
                                "example/SurfConventionsTest*",
                                "example/ProductionCodeBenchmark*",
                                "org/junit/**",
                                "io/mockk/**",
                                "org/jetbrains/lincheck/**",
                                "kotlinx/benchmark/**"
                            )
                        }
                        check(forbiddenShadedClasses.isEmpty)
                    }
                }
            """.trimIndent()
        )
        projectDir.writeFile(
            "src/main/kotlin/example/ProductionCode.kt",
            """
                package example

                import com.google.common.collect.ImmutableList

                object ProductionCode {
                    fun values(): List<String> = ImmutableList.of("available at test runtime")
                }
            """.trimIndent()
        )
        projectDir.writeFile(
            "src/test/kotlin/example/SurfConventionsTest.kt",
            """
                package example

                import io.mockk.Runs
                import io.mockk.every
                import io.mockk.just
                import io.mockk.mockk
                import io.mockk.verify
                import kotlinx.coroutines.test.runTest
                import org.jetbrains.lincheck.Lincheck
                import kotlin.test.Test
                import kotlin.test.assertEquals
                import kotlin.test.assertNotNull

                class SurfConventionsTest {
                    @Test
                    fun suppliedLibrariesAndCompileOnlyRuntimeAreUsable() = runTest {
                        val runnable = mockk<Runnable>()
                        every { runnable.run() } just Runs

                        runnable.run()

                        verify(exactly = 1) { runnable.run() }
                        assertEquals(listOf("available at test runtime"), ProductionCode.values())
                        assertNotNull(Lincheck::class.qualifiedName)
                    }
                }
            """.trimIndent()
        )
        projectDir.writeFile(
            "src/benchmark/kotlin/example/ProductionCodeBenchmark.kt",
            """
                package example

                import kotlinx.benchmark.Benchmark
                import kotlinx.benchmark.Measurement
                import kotlinx.benchmark.Scope
                import kotlinx.benchmark.State
                import kotlinx.benchmark.Warmup
                import java.util.concurrent.TimeUnit

                @State(Scope.Benchmark)
                @Warmup(iterations = 0)
                @Measurement(iterations = 1, time = 10, timeUnit = TimeUnit.MILLISECONDS)
                class ProductionCodeBenchmark {
                    @Benchmark
                    fun compileOnlyDependencyIsAvailable(): Int = ProductionCode.values().size
                }
            """.trimIndent()
        )

        val result = runner(
            "test",
            "benchmark",
            "verifySurfTestingConventions",
            "--stacktrace"
        ).build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":test")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":benchmarkBenchmark")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":verifySurfTestingConventions")?.outcome)
    }

    @Test
    fun `platform and optional dependencies propagate without changing production scopes`() {
        projectDir.writeFile(
            "settings.gradle.kts",
            """
                rootProject.name = "surf-platform-classpath-functional-test"
                include("core", "paper", "velocity", "standalone")
            """.trimIndent()
        )
        projectDir.writeFile(
            "build.gradle.kts",
            """
                import org.gradle.api.artifacts.Configuration

                fun Configuration.coordinates(): Set<String> = hierarchy
                    .flatMap { it.allDependencies }
                    .map { "${'$'}{it.group}:${'$'}{it.name}" }
                    .toSet()

                val expectedDependencies = mapOf(
                    "core" to setOf(
                        "dev.slne.surf.api:surf-api-core",
                        "dev.slne.surf.core:surf-core-api-common",
                        "dev.slne.surf.redis:surf-redis-api",
                        "dev.slne.surf:surf-database-r2dbc"
                    ),
                    "paper" to setOf(
                        "dev.slne.surf.api:surf-api-paper",
                        "io.canvasmc.canvas:canvas-api"
                    ),
                    "velocity" to setOf(
                        "dev.slne.surf.api:surf-api-velocity",
                        "com.velocitypowered:velocity-api"
                    ),
                    "standalone" to setOf(
                        "dev.slne.surf.api:surf-api-standalone"
                    )
                )

                subprojects {
                    tasks.register("verifyPlatformClasspath") {
                        doLast {
                            val expected = expectedDependencies.getValue(project.name)
                            listOf(
                                "testCompileClasspath",
                                "testRuntimeClasspath",
                                "benchmarkCompileClasspath",
                                "benchmarkRuntimeClasspath"
                            ).forEach { configurationName ->
                                val coordinates = configurations.getByName(configurationName).coordinates()
                                check(coordinates.containsAll(expected)) {
                                    "${'$'}configurationName in ${'$'}path is missing ${'$'}{expected - coordinates}"
                                }
                            }

                            val testOnlyGroups = setOf(
                                "org.junit.jupiter",
                                "org.junit.platform",
                                "io.mockk",
                                "org.jetbrains.lincheck"
                            )
                            listOf("api", "implementation", "compileOnly", "runtimeOnly").forEach { name ->
                                check(configurations.getByName(name).coordinates().none { coordinate ->
                                    testOnlyGroups.any { coordinate.startsWith("${'$'}it:") }
                                })
                            }
                        }
                    }
                }

                tasks.register("verifyPlatformClasspaths") {
                    dependsOn(subprojects.map { "${'$'}{it.path}:verifyPlatformClasspath" })
                }
            """.trimIndent()
        )
        projectDir.writeFile(
            "core/build.gradle.kts",
            """
                plugins {
                    id("dev.slne.surf.api.gradle.core")
                }

                surfCoreApi {
                    withCoreCommon()
                    withSurfRedis()
                    withSurfDatabaseR2dbc("+", "example.relocated.database")
                }
            """.trimIndent()
        )
        projectDir.writeFile(
            "paper/build.gradle.kts",
            """
                plugins {
                    id("dev.slne.surf.api.gradle.paper-raw")
                }

                surfRawPaperApi {
                    useCanvasMc()
                }
            """.trimIndent()
        )
        projectDir.writeFile(
            "velocity/build.gradle.kts",
            """
                plugins {
                    id("dev.slne.surf.api.gradle.velocity")
                }
            """.trimIndent()
        )
        projectDir.writeFile(
            "standalone/build.gradle.kts",
            """
                plugins {
                    id("dev.slne.surf.api.gradle.standalone")
                }
            """.trimIndent()
        )

        val result = runner("verifyPlatformClasspaths", "--stacktrace").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":verifyPlatformClasspaths")?.outcome)
    }

    @Test
    fun `Paper and Velocity APIs compile and load in tests and benchmarks`() {
        projectDir.writeFile(
            "settings.gradle.kts",
            """
                rootProject.name = "surf-platform-runtime-functional-test"
                include("paper", "velocity")
            """.trimIndent()
        )
        projectDir.writeFile(
            "paper/build.gradle.kts",
            """
                plugins {
                    id("dev.slne.surf.api.gradle.paper-raw")
                }

                surfRawPaperApi {
                    addSurfApiToClasspath(false)
                }
            """.trimIndent()
        )
        projectDir.writeFile(
            "paper/src/test/kotlin/example/PaperApiTest.kt",
            """
                package example

                import org.bukkit.Bukkit
                import kotlin.test.Test
                import kotlin.test.assertEquals

                class PaperApiTest {
                    @Test
                    fun paperApiLoadsAtTestRuntime() {
                        assertEquals("org.bukkit.Bukkit", Bukkit::class.java.name)
                    }
                }
            """.trimIndent()
        )
        projectDir.writeFile(
            "paper/src/benchmark/kotlin/example/PaperApiBenchmark.kt",
            """
                package example

                import kotlinx.benchmark.Benchmark
                import org.bukkit.Bukkit

                class PaperApiBenchmark {
                    @Benchmark
                    fun platformTypeIsOnBenchmarkClasspath(): String = Bukkit::class.java.name
                }
            """.trimIndent()
        )
        projectDir.writeFile(
            "velocity/build.gradle.kts",
            """
                plugins {
                    id("dev.slne.surf.api.gradle.velocity")
                }

                surfVelocityApi {
                    addSurfApiToClasspath(false)
                }
            """.trimIndent()
        )
        projectDir.writeFile(
            "velocity/src/test/kotlin/example/VelocityApiTest.kt",
            """
                package example

                import com.velocitypowered.api.proxy.ProxyServer
                import kotlin.test.Test
                import kotlin.test.assertEquals

                class VelocityApiTest {
                    @Test
                    fun velocityApiLoadsAtTestRuntime() {
                        assertEquals(
                            "com.velocitypowered.api.proxy.ProxyServer",
                            ProxyServer::class.java.name
                        )
                    }
                }
            """.trimIndent()
        )
        projectDir.writeFile(
            "velocity/src/benchmark/kotlin/example/VelocityApiBenchmark.kt",
            """
                package example

                import com.velocitypowered.api.proxy.ProxyServer
                import kotlinx.benchmark.Benchmark

                class VelocityApiBenchmark {
                    @Benchmark
                    fun platformTypeIsOnBenchmarkClasspath(): String =
                        ProxyServer::class.java.name
                }
            """.trimIndent()
        )

        val result = runner(
            ":paper:test",
            ":paper:compileBenchmarkKotlin",
            ":velocity:test",
            ":velocity:compileBenchmarkKotlin",
            "--stacktrace"
        ).build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":paper:test")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":paper:compileBenchmarkKotlin")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":velocity:test")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":velocity:compileBenchmarkKotlin")?.outcome)
    }

    @Test
    fun `consumer test build reuses the configuration cache`() {
        projectDir.writeFile(
            "settings.gradle.kts",
            """
                rootProject.name = "surf-configuration-cache-functional-test"
            """.trimIndent()
        )
        projectDir.writeFile(
            "build.gradle.kts",
            """
                plugins {
                    id("dev.slne.surf.api.gradle.core")
                }

                surfCoreApi {
                    addSurfApiToClasspath(false)
                }
            """.trimIndent()
        )
        projectDir.writeFile(
            "src/test/kotlin/example/ConfigurationCacheTest.kt",
            """
                package example

                import kotlin.test.Test
                import kotlin.test.assertTrue

                class ConfigurationCacheTest {
                    @Test
                    fun executes() {
                        assertTrue(true)
                    }
                }
            """.trimIndent()
        )

        runner("test", "--configuration-cache", "--stacktrace").build()
        val reused = runner("test", "--configuration-cache", "--stacktrace").build()

        assertEquals(TaskOutcome.UP_TO_DATE, reused.task(":test")?.outcome)
        assertTrue(reused.output.contains("Reusing configuration cache."))
    }

    private fun runner(vararg arguments: String): GradleRunner {
        val runnerArguments = arguments.toMutableList()
        val environment = System.getenv().toMutableMap()
        System.getenv("SURF_TEST_JAVA_HOME")?.let { javaHome ->
            val normalizedJavaHome = javaHome.replace('\\', '/')
            runnerArguments += "-Dorg.gradle.java.home=$normalizedJavaHome"
            val trustStoreOption =
                "-Djavax.net.ssl.trustStore=$normalizedJavaHome/lib/security/cacerts"
            runnerArguments += trustStoreOption
            environment["JAVA_HOME"] = javaHome
            environment["GRADLE_OPTS"] = listOfNotNull(
                environment["GRADLE_OPTS"],
                trustStoreOption
            ).filter(String::isNotBlank).joinToString(" ")
        }

        return GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments(runnerArguments)
            .withEnvironment(environment)
            .forwardOutput()
    }

    private fun Path.writeFile(relativePath: String, content: String) {
        val file = resolve(relativePath)
        file.parent?.createDirectories()
        file.writeText(content)
    }

}
