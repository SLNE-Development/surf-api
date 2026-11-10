package dev.slne.surf.microservice.gradle

import dev.slne.surf.microservice.gradle.testkit.TestBuild
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.util.jar.JarFile
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MicroservicePluginTest {

    @Test
    fun `standalone projects get api, runtime and processor with the plugin major version`(info: TestInfo) {
        val dependencies = standaloneBuild(info).run(SHOW_DEPENDENCIES).dependencies()

        assertContains(dependencies, "api dev.slne.surf.microservice:surf-microservice-api:3.+")
        assertContains(dependencies, "runtimeOnly dev.slne.surf.microservice:surf-microservice-runtime:3.+")
        assertContains(dependencies, "ksp dev.slne.surf.microservice:surf-microservice-processor:3.+")
    }

    @Test
    fun `database is enabled by default`(info: TestInfo) {
        val dependencies = standaloneBuild(info).run(SHOW_DEPENDENCIES).dependencies()

        assertContains(dependencies, "implementation dev.slne.surf.microservice:surf-microservice-database:3.+")
        assertContains(dependencies, "implementation dev.slne.surf:surf-database-r2dbc:2.+")
    }

    @Test
    fun `DSL overrides the database version after the plugin is applied`(info: TestInfo) {
        val build = standaloneBuild(info, "surfMicroservice { database { version = '2.4.0' } }")

        val dependencies = build.run(SHOW_DEPENDENCIES).dependencies()

        assertContains(dependencies, "implementation dev.slne.surf:surf-database-r2dbc:2.4.0")
    }

    @Test
    fun `Kotlin DSL configures the database and Docker settings`(info: TestInfo) {
        val build = TestBuild.create(info.displayName, withSurfApi = true)
        build.file(
            "build.gradle.kts",
            """
            plugins {
                id("dev.slne.surf.api.gradle.standalone")
                id("dev.slne.surf.microservice")
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

        assertContains(output, "surf-database-r2dbc:2.4.2")
        assertContains(output, "[setting] eclipse-temurin:25-jre-alpine")
    }

    @Test
    fun `Gradle properties override the database version`(info: TestInfo) {
        val dependencies = standaloneBuild(info)
            .run(SHOW_DEPENDENCIES, "-Psurf.database.version=2.4.1")
            .dependencies()

        assertContains(dependencies, "implementation dev.slne.surf:surf-database-r2dbc:2.4.1")
    }

    @Test
    fun `DSL disables the database`(info: TestInfo) {
        val dependencies = standaloneBuild(info, "surfMicroservice { database { enabled = false } }")
            .run(SHOW_DEPENDENCIES)
            .dependencies()

        assertTrue(dependencies.none { "surf-microservice-database" in it || "surf-database-r2dbc" in it }, "$dependencies")
        assertContains(dependencies, "api dev.slne.surf.microservice:surf-microservice-api:3.+")
    }

    @Test
    fun `Gradle property disables the database`(info: TestInfo) {
        val dependencies = standaloneBuild(info)
            .run(SHOW_DEPENDENCIES, "-Psurf.microservice.database.enabled=false")
            .dependencies()

        assertTrue(dependencies.none { "surf-database-r2dbc" in it }, "$dependencies")
    }

    @Test
    fun `invalid database property fails the build`(info: TestInfo) {
        val result = standaloneBuild(info).runAndFail(SHOW_DEPENDENCIES, "-Psurf.microservice.database.enabled=yes")

        assertContains(result.output, "'surf.microservice.database.enabled' must be 'true' or 'false', was 'yes'")
    }

    @Test
    fun `reliability is enabled by default with the surf-nats plugin`(info: TestInfo) {
        val dependencies = reliabilityBuild(info, withNatsPlugin = true).run(SHOW_RESOLVED).resolvedDependencies()

        assertContains(dependencies, "compileClasspath $RELIABILITY:3.+")
        assertContains(dependencies, "testCompileClasspath $RELIABILITY:3.+ [feature 'test-fixtures']")
    }

    @Test
    fun `reliability is enabled by default with surf-nats-standalone declared`(info: TestInfo) {
        val dependencies = reliabilityBuild(info, DECLARE_NATS).run(SHOW_RESOLVED).resolvedDependencies()

        assertContains(dependencies, "compileClasspath $RELIABILITY:3.+")
        assertContains(dependencies, "testCompileClasspath $RELIABILITY:3.+ [feature 'test-fixtures']")
    }

    @Test
    fun `reliability stays disabled by default without surf-nats`(info: TestInfo) {
        val dependencies = reliabilityBuild(info).run(SHOW_RESOLVED).resolvedDependencies()

        assertTrue(dependencies.none { RELIABILITY in it }, "$dependencies")
        assertContains(dependencies, "compileClasspath dev.slne.surf.microservice:surf-microservice-database:3.+")
    }

    @Test
    fun `reliability stays disabled by default without the database`(info: TestInfo) {
        val build = reliabilityBuild(info, "$DECLARE_NATS\nsurfMicroservice { database { enabled = false } }")

        val dependencies = build.run(SHOW_RESOLVED).resolvedDependencies()

        assertTrue(dependencies.none { RELIABILITY in it }, "$dependencies")
    }

    @Test
    fun `DSL disables reliability`(info: TestInfo) {
        val build = reliabilityBuild(info, "$DECLARE_NATS\nsurfMicroservice { reliability { enabled = false } }")

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
                id("dev.slne.surf.microservice")
            }
            dependencies {
                implementation("dev.slne.surf.nats:surf-nats-standalone:1.0.0")
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

        assertContains(result.output, "enables surf-microservice-reliability")
        assertContains(result.output, "surf-nats is missing. Apply the 'dev.slne.surf.nats' plugin")
    }

    @Test
    fun `reliability enabled by Gradle property without the database fails the build`(info: TestInfo) {
        val build = reliabilityBuild(info, "$DECLARE_NATS\nsurfMicroservice { database { enabled = false } }")

        val result = build.runAndFail(SHOW_RESOLVED, "-Psurf.microservice.reliability.enabled=true")

        assertContains(result.output, "Gradle property 'surf.microservice.reliability.enabled'")
        assertContains(result.output, "the database integration is disabled")
    }

    @Test
    fun `invalid reliability property fails the build`(info: TestInfo) {
        val result = reliabilityBuild(info, DECLARE_NATS)
            .runAndFail(SHOW_RESOLVED, "-Psurf.microservice.reliability.enabled=on")

        assertContains(result.output, "'surf.microservice.reliability.enabled' must be 'true' or 'false', was 'on'")
    }

    @Test
    fun `withSurfDatabaseR2dbc fails once the compile classpath resolves`(info: TestInfo) {
        val build = standaloneBuild(info, "surfStandaloneApi { withSurfDatabaseR2dbc('2.4.0', 'demo.libs.database') }")

        val result = build.runAndFail(RESOLVE_COMPILE_CLASSPATH)

        assertContains(result.output, "Execution failed for task ':resolveCompileClasspath'")
        assertContains(result.output, "Remove withSurfDatabaseR2dbc: surfMicroservice manages the database dependency")
        assertFalse("[resolved] compileClasspath" in result.output)
    }

    @Test
    fun `resolving the compile classpath passes without withSurfDatabaseR2dbc`(info: TestInfo) {
        val result = standaloneBuild(info).run(RESOLVE_COMPILE_CLASSPATH)

        assertContains(result.output, "[resolved] compileClasspath")
    }

    @Test
    fun `shadow jar uses the microservice launcher as main class`(info: TestInfo) {
        val build = shadowBuild(info)

        build.run("shadowJar")

        val manifest = JarFile(build.dir.resolve("build/libs/demo-all.jar")).use { jar -> jar.manifest }
        assertEquals(
            "dev.slne.surf.microservice.runtime.MicroserviceLauncherKt",
            manifest.mainAttributes.getValue("Main-Class"),
        )
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
        assertEquals(null, result.task(":shadowJar"), "generating the Dockerfile must not build the jar")
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
        val build = shadowBuild(info, "surfMicroservice { docker { jvmOptions.add('-Xlog:gc*:stdout') } }")

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
        val build = shadowBuild(info, "surfMicroservice { docker { jvmOptions = ['-Dvalue=\$(id)'] } }")

        val result = build.runAndFail("generateMicroserviceDockerfile")

        assertContains(result.output, "surfMicroservice.docker.jvmOptions: '-Dvalue=\$(id)' must start with '-'")
    }

    @Test
    fun `memory budget that is no size fails the generation`(info: TestInfo) {
        val build = shadowBuild(info, "surfMicroservice { docker { memoryBudget = '1 GB' } }")

        val result = build.runAndFail("generateMicroserviceDockerfile")

        assertContains(result.output, "surfMicroservice.docker.memoryBudget: '1 GB' is no memory size")
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
                id 'dev.slne.surf.microservice'
                id 'com.gradleup.shadow'
                id 'java-library'
            }
            tasks.register('showMainClass') {
                def mainClass = tasks.named('shadowJar').flatMap { it.mainClass }
                doLast { println "[main] " + mainClass.get() }
            }
            """
        )

        val result = build.run("showMainClass", "tasks", "--all")

        assertContains(result.output, "[main] dev.slne.surf.microservice.runtime.MicroserviceLauncherKt")
        assertContains(result.output, "runMicroservice")
        assertContains(result.output, "generateMicroserviceDockerfile")
    }

    @Test
    fun `standalone shadow jar uses the microservice launcher as main class`(info: TestInfo) {
        val build = standaloneBuild(
            info,
            """
            tasks.register('showMainClass') {
                def mainClass = tasks.named('shadowJar').flatMap { it.mainClass }
                doLast { println "[main] " + mainClass.get() }
            }
            """,
        )

        val result = build.run("showMainClass")

        assertContains(result.output, "[main] dev.slne.surf.microservice.runtime.MicroserviceLauncherKt")
    }

    @Test
    fun `runMicroservice is registered in the application group`(info: TestInfo) {
        val result = standaloneBuild(info).run("tasks", "--group", "application")

        assertContains(result.output, "runMicroservice - Builds the microservice jar and runs it in run/.")
    }

    @Test
    fun `applies without surf-api and Shadow on the classpath`(info: TestInfo) {
        val build = TestBuild.create(info.displayName, withSurfApi = false)
        build.script(
            """
            plugins {
                id 'java-library'
                id 'dev.slne.surf.microservice'
            }
            surfMicroservice { docker { overwrite = true } }
            $SHOW_DEPENDENCIES_TASK
            """
        )

        val result = build.run(SHOW_DEPENDENCIES, "tasks", "--all")

        assertContains(result.dependencies(), "api dev.slne.surf.microservice:surf-microservice-api:3.+")
        assertFalse("generateMicroserviceDockerfile" in result.output)
        assertFalse("runMicroservice" in result.output)
    }

    private fun standaloneBuild(info: TestInfo, configuration: String = ""): TestBuild =
        TestBuild.create(info.displayName, withSurfApi = true).apply {
            script(
                """
                plugins {
                    id 'dev.slne.surf.api.gradle.standalone'
                    id 'dev.slne.surf.microservice'
                }
                $configuration
                $SHOW_DEPENDENCIES_TASK
                $RESOLVE_COMPILE_CLASSPATH_TASK
                """
            )
        }

    /**
     * A plain `java-library` project; [withNatsPlugin] applies a no-op plugin with the surf-nats plugin id.
     */
    private fun reliabilityBuild(
        info: TestInfo,
        configuration: String = "",
        withNatsPlugin: Boolean = false,
    ): TestBuild = TestBuild.create(info.displayName, withSurfApi = false).apply {
        if (withNatsPlugin) fakeNatsPlugin()
        script(
            """
            plugins {
                id 'java-library'
                ${if (withNatsPlugin) "id 'dev.slne.surf.nats'" else ""}
                id 'dev.slne.surf.microservice'
            }
            $configuration
            $SHOW_RESOLVED_TASK
            """
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
                        id = 'dev.slne.surf.nats'
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
     * A plain Java project with Shadow whose surf dependencies are dropped, so its jar builds offline.
     */
    private fun shadowBuild(info: TestInfo, configuration: String = ""): TestBuild =
        TestBuild.create(info.displayName, withSurfApi = true).apply {
            script(
                """
                plugins {
                    id 'java-library'
                    id 'com.gradleup.shadow'
                    id 'dev.slne.surf.microservice'
                }
                ['api', 'implementation', 'runtimeOnly'].each { name ->
                    configurations.named(name) {
                        withDependencies { it.removeIf { dependency -> dependency.group?.startsWith('dev.slne.surf') } }
                    }
                }
                $configuration
                """
            )
        }

    private fun BuildResult.dependencies(): List<String> =
        output.lines().filter { it.startsWith(DEPENDENCY_PREFIX) }.map { it.removePrefix(DEPENDENCY_PREFIX) }

    private fun BuildResult.resolvedDependencies(): List<String> =
        output.lines().filter { it.startsWith(RESOLVED_PREFIX) }.map { it.removePrefix(RESOLVED_PREFIX) }

    private companion object {
        const val SHOW_DEPENDENCIES = "showDependencies"
        const val RESOLVE_COMPILE_CLASSPATH = "resolveCompileClasspath"
        const val DEPENDENCY_PREFIX = "[dependency] "
        const val SHOW_RESOLVED = "showResolvedDependencies"
        const val RESOLVED_PREFIX = "[resolved-dependency] "
        const val RELIABILITY = "dev.slne.surf.microservice:surf-microservice-reliability"
        const val DECLARE_NATS = "dependencies { implementation 'dev.slne.surf.nats:surf-nats-standalone:1.0.0' }"

        /**
         * Prints the dependencies of both compile classpaths after their `withDependencies` actions ran, without
         * resolving the graph; requested capabilities follow in brackets.
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
         * Prints the declared dependencies without resolving them.
         */
        const val SHOW_DEPENDENCIES_TASK = """
            tasks.register('showDependencies') {
                doLast {
                    ['api', 'implementation', 'runtimeOnly', 'ksp'].each { name ->
                        configurations.findByName(name)?.dependencies?.each { dependency ->
                            println "[dependency] ${'$'}name ${'$'}{dependency.group}:${'$'}{dependency.name}:${'$'}{dependency.version}"
                        }
                    }
                }
            }
        """

        /**
         * Resolves the dependency graph of `compileClasspath`; offline, unresolvable modules stay unresolved
         * instead of failing.
         */
        const val RESOLVE_COMPILE_CLASSPATH_TASK = """
            tasks.register('resolveCompileClasspath') {
                doLast {
                    configurations.compileClasspath.incoming.resolutionResult.root
                    println '[resolved] compileClasspath'
                }
            }
        """
    }
}
