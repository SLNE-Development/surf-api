package dev.slne.surf.microservice.gradle

import dev.slne.surf.microservice.gradle.generated.Constants
import dev.slne.surf.microservice.gradle.testkit.TestBuild
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MicroservicePluginTest {

    @Test
    fun `standalone projects get api and runtime with the plugin major version`(info: TestInfo) {
        val dependencies = standaloneBuild(info).run(SHOW_DEPENDENCIES).dependencies()

        assertContains(
            dependencies,
            "api dev.slne.surf.api:surf-api-microservice-api:$MICROSERVICE_VERSION",
        )
        assertContains(
            dependencies,
            "implementation dev.slne.surf.api:surf-api-microservice-runtime:$MICROSERVICE_VERSION",
        )
    }

    @Test
    fun `no KSP processor is added`(info: TestInfo) {
        val result = standaloneBuild(info).run(SHOW_ALL_DEPENDENCIES)

        assertFalse("surf-api-microservice-processor" in result.output)
    }

    @Test
    fun `database is enabled by default`(info: TestInfo) {
        val dependencies = standaloneBuild(info).run(SHOW_DEPENDENCIES).dependencies()

        assertContains(
            dependencies,
            "implementation dev.slne.surf.api:surf-api-microservice-database:$MICROSERVICE_VERSION",
        )
        assertContains(
            dependencies,
            "implementation dev.slne.surf.api:surf-api-database-r2dbc:$MICROSERVICE_VERSION",
        )
    }

    @Test
    fun `DSL overrides the database version after the plugin is applied`(info: TestInfo) {
        val build = standaloneBuild(info, "surfMicroservice { database { version = '2.4.0' } }")

        val dependencies = build.run(SHOW_DEPENDENCIES).dependencies()

        assertContains(
            dependencies,
            "implementation dev.slne.surf.api:surf-api-database-r2dbc:2.4.0",
        )
    }

    @Test
    fun `Kotlin DSL configures the database and Docker settings`(info: TestInfo) {
        val build = TestBuild.create(info.displayName, withSurfApi = true)
        build.file(
            "build.gradle.kts",
            """
            plugins {
                id("dev.slne.surf.api.gradle.standalone")
                id("dev.slne.surf.api.gradle.microservice")
            }
            surfMicroservice {
                database {
                    version = "2.4.2"
                }
                docker {
                    overwrite = true
                    baseImage = "eclipse-temurin:25-jre-alpine"
                }
            }
            tasks.register("showSettings") {
                val database = configurations.named("implementation").map { it.dependencies.map { d -> "${'$'}{d.name}:${'$'}{d.version}" } }
                val image = surfMicroservice.docker.baseImage
                doLast {
                    println("[setting] " + database.get())
                    println("[setting] " + image.get())
                }
            }
            """,
        )

        val output = build.run("showSettings").output

        assertContains(output, "surf-api-database-r2dbc:2.4.2")
        assertContains(output, "[setting] eclipse-temurin:25-jre-alpine")
    }

    @Test
    fun `Gradle properties override the database version`(info: TestInfo) {
        val dependencies = standaloneBuild(info)
            .run(SHOW_DEPENDENCIES, "-Psurf.database.version=2.4.1")
            .dependencies()

        assertContains(
            dependencies,
            "implementation dev.slne.surf.api:surf-api-database-r2dbc:2.4.1",
        )
    }

    @Test
    fun `DSL disables the database`(info: TestInfo) {
        val dependencies = standaloneBuild(
            info,
            "surfMicroservice { database { enabled = false } }",
        )
            .run(SHOW_DEPENDENCIES)
            .dependencies()

        assertTrue(
            dependencies.none { dependency ->
                "surf-api-microservice-database" in dependency ||
                    "surf-api-database-r2dbc" in dependency
            },
            "$dependencies",
        )
        assertContains(
            dependencies,
            "api dev.slne.surf.api:surf-api-microservice-api:$MICROSERVICE_VERSION",
        )
    }

    @Test
    fun `Gradle property disables the database`(info: TestInfo) {
        val dependencies = standaloneBuild(info)
            .run(SHOW_DEPENDENCIES, "-Psurf.microservice.database.enabled=false")
            .dependencies()

        assertTrue(dependencies.none { "surf-api-database-r2dbc" in it }, "$dependencies")
    }

    @Test
    fun `invalid database property fails the build`(info: TestInfo) {
        val result = standaloneBuild(info).runAndFail(
            SHOW_DEPENDENCIES,
            "-Psurf.microservice.database.enabled=yes",
        )

        assertContains(
            result.output,
            "'surf.microservice.database.enabled' must be 'true' or 'false', was 'yes'",
        )
    }

    @Test
    fun `reliability is enabled by default with the surf-nats plugin`(info: TestInfo) {
        val dependencies = reliabilityBuild(
            info,
            withNatsPlugin = true,
        ).run(SHOW_RESOLVED).resolvedDependencies()

        assertContains(dependencies, "compileClasspath $RELIABILITY:$MICROSERVICE_VERSION")
        assertContains(
            dependencies,
            "testCompileClasspath $RELIABILITY:$MICROSERVICE_VERSION [feature 'test-fixtures']",
        )
    }

    @Test
    fun `reliability is enabled by default with surf-api-nats-spring declared`(info: TestInfo) {
        val dependencies = reliabilityBuild(
            info,
            DECLARE_NATS,
        ).run(SHOW_RESOLVED).resolvedDependencies()

        assertContains(dependencies, "compileClasspath $RELIABILITY:$MICROSERVICE_VERSION")
        assertContains(
            dependencies,
            "testCompileClasspath $RELIABILITY:$MICROSERVICE_VERSION [feature 'test-fixtures']",
        )
    }

    @Test
    fun `reliability stays disabled by default without surf-nats`(info: TestInfo) {
        val dependencies = reliabilityBuild(info).run(SHOW_RESOLVED).resolvedDependencies()

        assertTrue(dependencies.none { RELIABILITY in it }, "$dependencies")
        assertContains(
            dependencies,
            "compileClasspath dev.slne.surf.api:surf-api-microservice-database:$MICROSERVICE_VERSION",
        )
    }

    @Test
    fun `reliability stays disabled by default without the database`(info: TestInfo) {
        val build = reliabilityBuild(
            info,
            "$DECLARE_NATS\nsurfMicroservice { database { enabled = false } }",
        )

        val dependencies = build.run(SHOW_RESOLVED).resolvedDependencies()

        assertTrue(dependencies.none { RELIABILITY in it }, "$dependencies")
    }

    @Test
    fun `DSL disables reliability`(info: TestInfo) {
        val build = reliabilityBuild(
            info,
            "$DECLARE_NATS\nsurfMicroservice { reliability { enabled = false } }",
        )

        val dependencies = build.run(SHOW_RESOLVED).resolvedDependencies()

        assertTrue(dependencies.none { RELIABILITY in it }, "$dependencies")
    }

    @Test
    fun `Gradle property disables reliability`(info: TestInfo) {
        val dependencies = reliabilityBuild(info, DECLARE_NATS)
            .run(SHOW_RESOLVED, "-Psurf.microservice.reliability.enabled=false")
            .resolvedDependencies()

        assertTrue(dependencies.none { RELIABILITY in it }, "$dependencies")
    }

    @Test
    fun `Kotlin DSL enables reliability`(info: TestInfo) {
        val build = TestBuild.create(info.displayName, withSurfApi = false)
        build.file(
            "build.gradle.kts",
            """
            plugins {
                `java-library`
                id("dev.slne.surf.api.gradle.microservice")
            }
            dependencies {
                implementation("dev.slne.surf.api:surf-api-nats-spring:1.0.0")
            }
            surfMicroservice {
                reliability {
                    enabled = true
                }
            }
            tasks.register("showReliability") {
                val enabled = surfMicroservice.reliability.enabled
                doLast { println("[setting] reliability=" + enabled.get()) }
            }
            """,
        )

        val output = build.run("showReliability").output

        assertContains(output, "[setting] reliability=true")
    }

    @Test
    fun `explicitly enabled reliability without surf-nats fails the build`(info: TestInfo) {
        val build = reliabilityBuild(info, "surfMicroservice { reliability { enabled = true } }")

        val result = build.runAndFail(SHOW_RESOLVED)

        assertContains(result.output, "enables surf-api-microservice-reliability")
        assertContains(
            result.output,
            "surf-nats is missing. Apply the 'dev.slne.surf.api.gradle.nats' plugin",
        )
    }

    @Test
    fun `reliability enabled by Gradle property without the database fails the build`(info: TestInfo) {
        val build = reliabilityBuild(
            info,
            "$DECLARE_NATS\nsurfMicroservice { database { enabled = false } }",
        )

        val result = build.runAndFail(SHOW_RESOLVED, "-Psurf.microservice.reliability.enabled=true")

        assertContains(result.output, "Gradle property 'surf.microservice.reliability.enabled'")
        assertContains(result.output, "the database integration is disabled")
    }

    @Test
    fun `invalid reliability property fails the build`(info: TestInfo) {
        val result = reliabilityBuild(info, DECLARE_NATS)
            .runAndFail(SHOW_RESOLVED, "-Psurf.microservice.reliability.enabled=on")

        assertContains(
            result.output,
            "'surf.microservice.reliability.enabled' must be 'true' or 'false', was 'on'",
        )
    }

    @Test
    fun `shadow jar takes the single class with a main method as main class`(info: TestInfo) {
        val build = shadowBuild(info)
        build.javaClass("demo.Application")
        build.javaClass("demo.Launcher", "public static void main(String[] args) {}")
        build.javaClass("demo.InstanceMain", "public void main(String[] args) {}")
        build.javaClass("demo.HiddenMain", "static void main(String[] args) {}")
        build.javaClass("demo.WrongSignature", "public static void main(String arg) {}")

        val result = build.run("shadowJar")

        assertEquals(TaskOutcome.SUCCESS, result.task(":detectMicroserviceMainClass")?.outcome)
        assertEquals("demo.Launcher", build.mainClassOfShadowJar())
    }

    @Test
    fun `shadow jar uses the main class from the DSL even if the classes have several`(info: TestInfo) {
        val build = shadowBuild(info, "surfMicroservice { mainClass = 'demo.Second' }")
        build.javaClass("demo.First", "public static void main(String[] args) {}")
        build.javaClass("demo.Second", "public static void main(String[] args) {}")

        build.run("shadowJar")

        assertEquals("demo.Second", build.mainClassOfShadowJar())
    }

    @Test
    fun `main class set on the shadow jar skips the detection`(info: TestInfo) {
        val build = shadowBuild(info, "tasks.named('shadowJar') { mainClass = 'demo.Custom' }")
        build.javaClass("demo.First", "public static void main(String[] args) {}")
        build.javaClass("demo.Second", "public static void main(String[] args) {}")

        val result = build.run("shadowJar")

        assertEquals(
            null,
            result.task(":detectMicroserviceMainClass"),
            "the detection must not run",
        )
        assertEquals("demo.Custom", build.mainClassOfShadowJar())
    }

    @Test
    fun `missing main class fails the shadow jar with a hint`(info: TestInfo) {
        val build = shadowBuild(info)
        build.javaClass("demo.Application")

        val result = build.runAndFail("shadowJar")

        assertContains(result.output, "Execution failed for task ':detectMicroserviceMainClass'")
        assertContains(result.output, "No main class found in the compiled classes of ':'")
        assertContains(
            result.output,
            "fun main(args: Array<String>) { runMicroservice<App>(args) }",
        )
        assertContains(result.output, "surfMicroservice { mainClass = \"...\" }")
    }

    @Test
    fun `several main classes fail the shadow jar and name them`(info: TestInfo) {
        val build = shadowBuild(info)
        build.javaClass("demo.Beta", "public static void main(String[] args) {}")
        build.javaClass("demo.Alpha", "public static void main(String[] args) {}")

        val result = build.runAndFail("shadowJar")

        assertContains(result.output, "Found 2 main classes in ':': demo.Alpha, demo.Beta")
        assertContains(result.output, "surfMicroservice { mainClass = \"...\" }")
    }

    @Test
    fun `main class detection is reused while the classes stay the same`(info: TestInfo) {
        val build = shadowBuild(info)
        build.javaClass("demo.Launcher", "public static void main(String[] args) {}")
        build.run("shadowJar", "--configuration-cache")

        val result = build.run("shadowJar", "--configuration-cache")

        assertContains(result.output, "Reusing configuration cache.")
        assertEquals(TaskOutcome.UP_TO_DATE, result.task(":detectMicroserviceMainClass")?.outcome)
    }

    @Test
    fun `shadow jar merges service files, imports files and spring factories of all jars`(info: TestInfo) {
        val build = shadowBuild(
            info,
            """
            surfMicroservice { mainClass = 'demo.Main' }
            dependencies { runtimeOnly files('libs/first.jar', 'libs/second.jar') }
            tasks.named('shadowJar') { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
            """,
        )
        build.file(
            "src/main/resources/META-INF/spring.factories",
            "org.springframework.context.ApplicationContextInitializer=demo.OwnInitializer",
        )
        build.jar(
            "libs/first.jar",
            "META-INF/spring.factories" to """
                org.springframework.context.ApplicationContextInitializer=first.Initializer
                org.first.Only=first.Only
            """,
            "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports" to """
                # first
                first.AutoA
                first.AutoB
                shared.Auto
            """,
            "META-INF/spring/aot.factories" to "org.springframework.aot.hint.RuntimeHintsRegistrar=first.Hints",
            "META-INF/services/demo.Service" to "first.Impl",
            "META-INF/spring.handlers" to """
                http\://first.example/schema=first.Handler
                http\://shared.example/schema=first.SharedHandler
            """,
            "shared/Resource.txt" to "first",
        )
        build.jar(
            "libs/second.jar",
            "META-INF/spring.factories" to """
                org.springframework.context.ApplicationContextInitializer=second.Initializer,second.Other
                org.second.Only=second.Only
            """,
            "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports" to """
                second.AutoC
                shared.Auto
            """,
            "META-INF/spring/org.example.Custom.imports" to "second.Custom",
            "META-INF/spring/aot.factories" to "org.springframework.aot.hint.RuntimeHintsRegistrar=second.Hints",
            "META-INF/services/demo.Service" to "second.Impl",
            "META-INF/spring.handlers" to """
                http\://second.example/schema=second.Handler
                http\://shared.example/schema=second.SharedHandler
            """,
            "shared/Resource.txt" to "second",
        )

        build.run("shadowJar")

        JarFile(build.shadowJarFile()).use { jar ->
            val factories = jar.properties("META-INF/spring.factories")
            assertEquals(
                setOf(
                    "demo.OwnInitializer",
                    "first.Initializer",
                    "second.Initializer",
                    "second.Other",
                ),
                factories.values("org.springframework.context.ApplicationContextInitializer"),
            )
            assertEquals(setOf("first.Only"), factories.values("org.first.Only"))
            assertEquals(setOf("second.Only"), factories.values("org.second.Only"))

            assertEquals(
                setOf("first.Hints", "second.Hints"),
                jar.properties("META-INF/spring/aot.factories")
                    .values("org.springframework.aot.hint.RuntimeHintsRegistrar"),
            )

            val autoConfigurations = jar.lines(
                "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports",
            )
            assertEquals(
                listOf("first.AutoA", "first.AutoB", "second.AutoC", "shared.Auto"),
                autoConfigurations.filterNot { it.startsWith("#") }.sorted(),
            )
            assertEquals(
                listOf("second.Custom"),
                jar.lines("META-INF/spring/org.example.Custom.imports"),
            )
            assertEquals(
                setOf("first.Impl", "second.Impl"),
                jar.lines("META-INF/services/demo.Service").toSet(),
            )
            assertEquals(
                setOf("first.Handler", "second.Handler"),
                jar.properties("META-INF/spring.handlers").values(
                    "http://first.example/schema",
                    "http://second.example/schema",
                ),
            )
            assertEquals(
                "first.SharedHandler",
                jar.properties("META-INF/spring.handlers")
                    .getProperty("http://shared.example/schema"),
                "a handler key keeps one class name instead of a comma-joined list",
            )
            assertEquals(
                1,
                jar.entries().asSequence().count { it.name == "shared/Resource.txt" },
                "other duplicates still follow the duplicates strategy of the task",
            )
        }
    }

    @Test
    fun `shadow jar combines the log4j plugin caches of all jars`(info: TestInfo) {
        val build = shadowBuild(
            info,
            """
            surfMicroservice { mainClass = 'demo.Main' }
            dependencies { runtimeOnly files('libs/core.jar', 'libs/boot.jar') }
            """,
        )
        build.dir.resolve("libs").mkdirs()
        writeJar(
            build.dir.resolve("libs/core.jar"),
            LOG4J_PLUGIN_CACHE to pluginCache("core.Console" to "Appender"),
        )
        writeJar(
            build.dir.resolve("libs/boot.jar"),
            LOG4J_PLUGIN_CACHE to pluginCache("boot.Factory" to "ConfigurationFactory"),
        )

        build.run("shadowJar")

        JarFile(build.shadowJarFile()).use { jar ->
            val classes = jar.pluginClasses(LOG4J_PLUGIN_CACHE)
            assertEquals(
                setOf("core.Console", "boot.Factory"),
                classes,
                "each jar keeps the plugins of its own cache",
            )
        }
    }

    /**
     * Writes a log4j plugin cache in the format of `PluginCache`: the number of categories, then
     * per category its name, the number of its plugins and each plugin's key, class name, name,
     * printable flag and defer flag.
     */
    private fun pluginCache(vararg plugins: Pair<String, String>): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeInt(plugins.size)

            for ((className, category) in plugins) {
                out.writeUTF(category.lowercase())
                out.writeInt(1)
                out.writeUTF(className.substringAfterLast('.').lowercase())
                out.writeUTF(className)
                out.writeUTF(className.substringAfterLast('.'))
                out.writeBoolean(false)
                out.writeBoolean(false)
            }
        }

        return bytes.toByteArray()
    }

    private fun writeJar(file: File, vararg entries: Pair<String, ByteArray>) {
        JarOutputStream(file.outputStream()).use { jar ->
            for ((name, content) in entries) {
                jar.putNextEntry(JarEntry(name))
                jar.write(content)
                jar.closeEntry()
            }
        }
    }

    private fun JarFile.pluginClasses(entry: String): Set<String> {
        val jarEntry = checkNotNull(getJarEntry(entry)) { "$entry is missing from ${name}" }
        DataInputStream(getInputStream(jarEntry).buffered()).use { input ->
            return buildSet {
                repeat(input.readInt()) {
                    input.readUTF()
                    repeat(input.readInt()) {
                        input.readUTF()
                        add(input.readUTF())
                        input.readUTF()
                        input.readBoolean()
                        input.readBoolean()
                    }
                }
            }
        }
    }

    @Test
    fun `generated Dockerfile has a healthcheck and a stop signal`(info: TestInfo) {
        val build = shadowBuild(info)

        val result = build.run("generateMicroserviceDockerfile", "--configuration-cache")

        val dockerfile = build.dir.resolve("Dockerfile").readText()
        assertContains(
            dockerfile,
            """HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 CMD ["microservicectl", "health"]""",
        )
        assertContains(dockerfile, "STOPSIGNAL SIGTERM")
        assertContains(dockerfile, "./gradlew :prepareMicroserviceDockerArtifact --no-daemon")
        assertEquals(
            null,
            result.task(":shadowJar"),
            "generating the Dockerfile must not build the jar",
        )
    }

    @Test
    fun `generated Dockerfile starts java through the entrypoint with the default JVM settings`(info: TestInfo) {
        val build = shadowBuild(info)

        build.run("generateMicroserviceDockerfile")

        val dockerfile = build.dir.resolve("Dockerfile").readText()
        assertContains(dockerfile, """ENV SURF_MICROSERVICE_MEMORY_BUDGET="1g"""")
        assertContains(dockerfile, """ENV MALLOC_ARENA_MAX="2"""")
        assertContains(dockerfile, "RUN printf '%s\\n' \\")
        assertContains(dockerfile, "        > /usr/local/bin/microservice-entrypoint \\")
        assertContains(dockerfile, "    done < /proc/self/cgroup' \\")
        assertContains(
            dockerfile,
            "'exec java -XX:+UseG1GC -XX:+UseCompactObjectHeaders -XX:MaxRAMPercentage=60.0 " +
                "-XX:ActiveProcessorCount=2 -XX:G1PeriodicGCInterval=60000 -XX:TrimNativeHeapInterval=60000 " +
                "-XX:+ExitOnOutOfMemoryError \$budget \$JAVA_OPTS -jar /app/app.jar \"\$@\"'",
        )
        assertContains(dockerfile, """ENTRYPOINT ["/usr/local/bin/microservice-entrypoint"]""")
    }

    @Test
    fun `adding a JVM option keeps the defaults`(info: TestInfo) {
        val build = shadowBuild(
            info,
            "surfMicroservice { docker { jvmOptions.add('-Xlog:gc*:stdout') } }",
        )

        build.run("generateMicroserviceDockerfile")

        val dockerfile = build.dir.resolve("Dockerfile").readText()
        assertContains(dockerfile, "-XX:+ExitOnOutOfMemoryError -Xlog:gc*:stdout \$budget")
    }

    @Test
    fun `DSL replaces the JVM options and turns the memory budget off`(info: TestInfo) {
        val build = shadowBuild(
            info,
            "surfMicroservice { docker { jvmOptions = ['-XX:+UseSerialGC', '-Xss512k']; memoryBudget = '' } }",
        )

        build.run("generateMicroserviceDockerfile")

        val dockerfile = build.dir.resolve("Dockerfile").readText()
        assertContains(dockerfile, """ENV SURF_MICROSERVICE_MEMORY_BUDGET=""""")
        assertContains(dockerfile, "exec java -XX:+UseSerialGC -Xss512k \$budget \$JAVA_OPTS -jar")
    }

    @Test
    fun `JVM option with shell metacharacters fails the generation`(info: TestInfo) {
        val build = shadowBuild(
            info,
            "surfMicroservice { docker { jvmOptions = ['-Dvalue=\$(id)'] } }",
        )

        val result = build.runAndFail("generateMicroserviceDockerfile")

        assertContains(
            result.output,
            "surfMicroservice.docker.jvmOptions: '-Dvalue=\$(id)' must start with '-'",
        )
    }

    @Test
    fun `memory budget that is no size fails the generation`(info: TestInfo) {
        val build = shadowBuild(info, "surfMicroservice { docker { memoryBudget = '1 GB' } }")

        val result = build.runAndFail("generateMicroserviceDockerfile")

        assertContains(
            result.output,
            "surfMicroservice.docker.memoryBudget: '1 GB' is no memory size",
        )
    }

    @Test
    fun `generated Dockerfile is reused from the configuration cache`(info: TestInfo) {
        val build = shadowBuild(info)
        build.run("generateMicroserviceDockerfile", "--configuration-cache")

        val result = build.run("generateMicroserviceDockerfile", "--configuration-cache")

        assertContains(result.output, "Reusing configuration cache.")
    }

    @Test
    fun `existing hand-written Dockerfile is not overwritten`(info: TestInfo) {
        val build = shadowBuild(info)
        build.file("Dockerfile", "FROM scratch")

        val result = build.runAndFail("generateMicroserviceDockerfile")

        assertContains(result.output, "Refusing to overwrite")
        assertEquals("FROM scratch\n", build.dir.resolve("Dockerfile").readText())
    }

    @Test
    fun `runMicroservice runs the shadow jar in the run directory`(info: TestInfo) {
        val build = shadowBuild(info, "tasks.named('shadowJar') { mainClass = 'demo.Main' }")
        build.file(
            "src/main/java/demo/Main.java",
            """
            package demo;

            public final class Main {
                public static void main(String[] args) {
                    System.out.println("[run] workingDir=" + System.getProperty("user.dir"));
                }
            }
            """,
        )

        val result = build.run("runMicroservice", "--configuration-cache")

        assertEquals(TaskOutcome.SUCCESS, result.task(":shadowJar")?.outcome)
        assertContains(result.output, "[run] workingDir=${build.dir.resolve("run").canonicalPath}")

        val rerun = build.run("runMicroservice", "--configuration-cache")

        assertContains(rerun.output, "Reusing configuration cache.")
        assertContains(rerun.output, "[run] workingDir=")
    }

    @Test
    fun `plugin applied before Java and Shadow still configures the shadow jar`(info: TestInfo) {
        val build = TestBuild.create(info.displayName, withSurfApi = true)
        build.script(
            """
            plugins {
                id 'dev.slne.surf.api.gradle.microservice'
                id 'com.gradleup.shadow'
                id 'java-library'
            }
            $DROP_SURF_DEPENDENCIES
            """,
        )
        build.javaClass("demo.Launcher", "public static void main(String[] args) {}")

        val result = build.run("shadowJar", "tasks", "--all")

        assertEquals("demo.Launcher", build.mainClassOfShadowJar())
        assertContains(result.output, "runMicroservice")
        assertContains(result.output, "generateMicroserviceDockerfile")
    }

    @Test
    fun `application plugin applied after the microservice plugin keeps the detected main class`(
        info: TestInfo,
    ) {
        val build = applicationBuild(
            info,
            plugins = listOf("com.gradleup.shadow", MICROSERVICE_PLUGIN, "application"),
        )
        build.javaClass("demo.Launcher", "public static void main(String[] args) {}")

        build.run("shadowJar")

        assertEquals("demo.Launcher", build.mainClassOfShadowJar())
    }

    @Test
    fun `application plugin applied before the microservice plugin keeps the detected main class`(
        info: TestInfo,
    ) {
        val build = applicationBuild(
            info,
            plugins = listOf("application", "com.gradleup.shadow", MICROSERVICE_PLUGIN),
        )
        build.javaClass("demo.Launcher", "public static void main(String[] args) {}")

        build.run("shadowJar")

        assertEquals("demo.Launcher", build.mainClassOfShadowJar())
    }

    @Test
    fun `application main class set by the build is used when the DSL sets none`(info: TestInfo) {
        val build = applicationBuild(
            info,
            plugins = listOf("com.gradleup.shadow", MICROSERVICE_PLUGIN, "application"),
            configuration = "application { mainClass = 'demo.Custom' }",
        )
        build.javaClass("demo.First", "public static void main(String[] args) {}")
        build.javaClass("demo.Second", "public static void main(String[] args) {}")

        build.run("shadowJar")

        assertEquals("demo.Custom", build.mainClassOfShadowJar())
    }

    @Test
    fun `standalone projects with Shadow register the main class detection`(info: TestInfo) {
        val result = standaloneBuild(info).run("tasks", "--all")

        assertContains(
            result.output,
            "detectMicroserviceMainClass - Resolves the main class of the microservice.",
        )
    }

    @Test
    fun `runMicroservice is registered in the application group`(info: TestInfo) {
        val result = standaloneBuild(info).run("tasks", "--group", "application")

        assertContains(
            result.output,
            "runMicroservice - Builds the microservice jar and runs it in run/.",
        )
    }

    @Test
    fun `applies without surf-api and Shadow on the classpath`(info: TestInfo) {
        val build = TestBuild.create(info.displayName, withSurfApi = false)
        build.script(
            """
            plugins {
                id 'java-library'
                id 'dev.slne.surf.api.gradle.microservice'
            }
            surfMicroservice { docker { overwrite = true } }
            $SHOW_DEPENDENCIES_TASK
            """,
        )

        val result = build.run(SHOW_DEPENDENCIES, "tasks", "--all")

        assertContains(
            result.dependencies(),
            "api dev.slne.surf.api:surf-api-microservice-api:$MICROSERVICE_VERSION",
        )
        assertFalse("generateMicroserviceDockerfile" in result.output)
        assertFalse("runMicroservice" in result.output)
    }

    private fun standaloneBuild(info: TestInfo, configuration: String = ""): TestBuild {
        return TestBuild.create(info.displayName, withSurfApi = true).apply {
            script(
                """
                plugins {
                    id 'dev.slne.surf.api.gradle.standalone'
                    id 'dev.slne.surf.api.gradle.microservice'
                }
                $configuration
                $SHOW_DEPENDENCIES_TASK
                $SHOW_ALL_DEPENDENCIES_TASK
                """,
            )
        }
    }

    /**
     * A plain `java-library` project; [withNatsPlugin] applies a no-op plugin with the surf-nats
     * plugin id.
     */
    private fun reliabilityBuild(
        info: TestInfo,
        configuration: String = "",
        withNatsPlugin: Boolean = false,
    ): TestBuild = TestBuild.create(info.displayName, withSurfApi = false).apply {
        if (withNatsPlugin) {
            fakeNatsPlugin()
        }

        script(
            """
            plugins {
                id 'java-library'
                ${if (withNatsPlugin) "id 'dev.slne.surf.api.gradle.nats'" else ""}
                id 'dev.slne.surf.api.gradle.microservice'
            }
            $configuration
            $SHOW_RESOLVED_TASK
            """,
        )
    }

    private fun TestBuild.fakeNatsPlugin() {
        file(
            "buildSrc/build.gradle",
            """
            plugins { id 'java-gradle-plugin' }
            gradlePlugin {
                plugins {
                    nats {
                        id = 'dev.slne.surf.api.gradle.nats'
                        implementationClass = 'FakeNatsPlugin'
                    }
                }
            }
            """,
        )
        file(
            "buildSrc/src/main/java/FakeNatsPlugin.java",
            """
            import org.gradle.api.Plugin;
            import org.gradle.api.Project;

            public class FakeNatsPlugin implements Plugin<Project> {
                @Override
                public void apply(Project project) {
                }
            }
            """,
        )
    }

    /**
     * A plain Java project with Shadow whose surf dependencies are dropped, so its jar builds
     * offline.
     */
    private fun shadowBuild(info: TestInfo, configuration: String = ""): TestBuild {
        return TestBuild.create(info.displayName, withSurfApi = true).apply {
            script(
                """
                plugins {
                    id 'java-library'
                    id 'com.gradleup.shadow'
                    id 'dev.slne.surf.api.gradle.microservice'
                }
                $DROP_SURF_DEPENDENCIES
                $configuration
                """,
            )
        }
    }

    /**
     * A plain Java project that applies [plugins] in the given order, one of them `application`.
     */
    private fun applicationBuild(
        info: TestInfo,
        plugins: List<String>,
        configuration: String = "",
    ): TestBuild {
        return TestBuild.create(info.displayName, withSurfApi = true).apply {
            val pluginIds = plugins.joinToString(separator = "; ") { "id '$it'" }
            script(
                """
                plugins {
                    id 'java-library'
                    $pluginIds
                }
                $DROP_SURF_DEPENDENCIES
                $configuration
                """,
            )
        }
    }

    private fun TestBuild.shadowJarFile() = dir.resolve("build/libs/demo-all.jar")

    private fun TestBuild.mainClassOfShadowJar(): String? {
        return JarFile(shadowJarFile()).use { jar ->
            jar.manifest.mainAttributes.getValue("Main-Class")
        }
    }

    private fun JarFile.lines(entry: String): List<String> {
        return readEntry(entry).lines().filter { it.isNotBlank() }
    }

    private fun JarFile.properties(entry: String): Properties {
        return Properties().apply { load(readEntry(entry).reader()) }
    }

    private fun Properties.values(vararg keys: String): Set<String> {
        return keys
            .flatMap { key -> getProperty(key).split(',') }
            .toSet()
    }

    private fun JarFile.readEntry(entry: String): String {
        val jarEntry = checkNotNull(getJarEntry(entry)) { "$entry is missing from ${name}" }

        return getInputStream(jarEntry).use { it.readBytes().decodeToString() }
    }

    private fun BuildResult.dependencies(): List<String> {
        return output.lines().filter { it.startsWith(DEPENDENCY_PREFIX) }.map {
            it.removePrefix(
                DEPENDENCY_PREFIX,
            )
        }
    }

    private fun BuildResult.resolvedDependencies(): List<String> {
        return output.lines().filter { it.startsWith(RESOLVED_PREFIX) }.map {
            it.removePrefix(
                RESOLVED_PREFIX,
            )
        }
    }

    private companion object {
        const val LOG4J_PLUGIN_CACHE =
            "META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat"

        const val MICROSERVICE_VERSION = Constants.SURF_MICROSERVICE_VERSION
        const val MICROSERVICE_PLUGIN = "dev.slne.surf.api.gradle.microservice"
        const val SHOW_DEPENDENCIES = "showDependencies"
        const val SHOW_ALL_DEPENDENCIES = "showAllDependencies"
        const val DEPENDENCY_PREFIX = "[dependency] "
        const val SHOW_RESOLVED = "showResolvedDependencies"
        const val RESOLVED_PREFIX = "[resolved-dependency] "
        const val RELIABILITY = "dev.slne.surf.api:surf-api-microservice-reliability"
        const val DECLARE_NATS =
            "dependencies { implementation 'dev.slne.surf.api:surf-api-nats-spring:1.0.0' }"

        /**
         * Prints the dependencies of both compile classpaths after their `withDependencies` actions
         * ran, without resolving the graph; requested capabilities follow in brackets.
         */
        const val SHOW_RESOLVED_TASK = """
            tasks.register('showResolvedDependencies') {
                doLast {
                    ['compileClasspath', 'testCompileClasspath'].each { name ->
                        configurations.getByName(name).incoming.dependencies.each { dependency ->
                            def capabilities = dependency.capabilitySelectors.collect { it.displayName }
                            def suffix = capabilities.empty ? '' : " ${'$'}capabilities"
                            println "[resolved-dependency] ${'$'}name ${'$'}{dependency.group}:${'$'}{dependency.name}:${'$'}{dependency.version}${'$'}suffix"
                        }
                    }
                }
            }
        """

        /**
         * Drops the surf modules the plugin adds, so that the jar builds offline.
         */
        const val DROP_SURF_DEPENDENCIES = """
            ['api', 'implementation', 'runtimeOnly'].each { name ->
                configurations.named(name) {
                    withDependencies { it.removeIf { dependency -> (dependency.group?.startsWith('dev.slne.surf') ?: false) } }
                }
            }
        """

        /**
         * Prints the declared dependencies of every configuration without resolving them.
         */
        const val SHOW_ALL_DEPENDENCIES_TASK = """
            tasks.register('showAllDependencies') {
                doLast {
                    configurations.each { configuration ->
                        configuration.dependencies.each { dependency ->
                            println "[any-dependency] ${'$'}{configuration.name} ${'$'}{dependency.group}:${'$'}{dependency.name}"
                        }
                    }
                }
            }
        """

        /**
         * Prints the declared dependencies without resolving them.
         */
        const val SHOW_DEPENDENCIES_TASK = """
            tasks.register('showDependencies') {
                doLast {
                    ['api', 'implementation', 'runtimeOnly'].each { name ->
                        configurations.findByName(name)?.dependencies?.each { dependency ->
                            println "[dependency] ${'$'}name ${'$'}{dependency.group}:${'$'}{dependency.name}:${'$'}{dependency.version}"
                        }
                    }
                }
            }
        """
    }
}
