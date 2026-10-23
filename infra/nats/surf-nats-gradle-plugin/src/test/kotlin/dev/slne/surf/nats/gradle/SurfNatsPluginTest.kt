package dev.slne.surf.nats.gradle

import dev.slne.surf.api.gradle.platform.common.CommonSurfExtension
import dev.slne.surf.nats.gradle.generator.DefaultTypeMappings
import dev.slne.surf.nats.gradle.generator.GeneratedVisibility
import dev.slne.surf.nats.gradle.generator.TypeMapping
import dev.slne.surf.nats.gradle.tasks.CheckSurfNatsCompatibilityTask
import dev.slne.surf.nats.gradle.tasks.GenerateSurfNatsKotlinTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.attributes.Usage
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SurfNatsPluginTest {

    @TempDir
    lateinit var projectDir: File

    private fun project(vararg plugins: String, dir: File = projectDir): Project {
        // Resolving keeps files of the Gradle user home open on Windows, so it lives outside the deleted temp dir.
        val project = ProjectBuilder.builder().withProjectDir(dir).withGradleUserHomeDir(File("build/tmp/projectBuilderUserHome")).build()
        for (plugin in plugins) {
            if (plugin == SurfNatsPlugin.ID) project.pluginManager.apply(SurfNatsPlugin::class.java)
            else project.pluginManager.apply(plugin)
        }
        return project
    }

    private val Project.surfNats: SurfNatsExtension get() = extensions.getByType(SurfNatsExtension::class.java)
    private val Project.generate: GenerateSurfNatsKotlinTask
        get() = tasks.getByName(SurfNatsPlugin.GENERATE_TASK) as GenerateSurfNatsKotlinTask
    private val Project.check: CheckSurfNatsCompatibilityTask
        get() = tasks.getByName(SurfNatsPlugin.CHECK_TASK) as CheckSurfNatsCompatibilityTask

    @Test
    fun `applies to a project without Kotlin and surf-api`() {
        val project = project(SurfNatsPlugin.ID)
        assertNotNull(project.extensions.findByName(SurfNatsPlugin.EXTENSION))
        assertNotNull(project.tasks.findByName(SurfNatsPlugin.GENERATE_TASK))
        assertNotNull(project.tasks.findByName(SurfNatsPlugin.CHECK_TASK))
        assertNotNull(project.configurations.findByName(SurfNatsPlugin.PROTO_PATH_CONFIGURATION))
        assertNotNull(project.configurations.findByName(SurfNatsPlugin.BASELINE_CONFIGURATION))
    }

    @Test
    fun `has the documented defaults`() {
        val project = project(KOTLIN_JVM, SurfNatsPlugin.ID)
        assertEquals(setOf(projectDir.resolve("src/main/proto")), project.surfNats.protoSourceDirs.files)
        assertEquals(GeneratedVisibility.PUBLIC, project.surfNats.generatedVisibility.get())
        assertTrue(project.surfNats.compatibilityCheck.get())
        assertFalse(project.surfNats.compatibilityBaseline.isPresent)
        assertEquals(projectDir.resolve("build/generated/surf-nats/kotlin"), project.generate.outputDir.get().asFile)
        assertEquals(DefaultTypeMappings.common, project.generate.defaultTypeMappings.get())
        assertFalse(project.check.baselineConfigured.get())
    }

    @Test
    fun `from adds to the default proto source directory and setFrom replaces it`() {
        val project = project(KOTLIN_JVM, SurfNatsPlugin.ID)
        project.surfNats.protoSourceDirs.from("src/extra/proto")
        assertEquals(
            setOf(projectDir.resolve("src/main/proto"), projectDir.resolve("src/extra/proto")),
            project.generate.sourceDirectories.files,
        )
        project.surfNats.protoSourceDirs.setFrom("contracts")
        assertEquals(setOf(projectDir.resolve("contracts")), project.generate.sourceDirectories.files)
    }

    @Test
    fun `passes the DSL values to the tasks`() {
        val project = project(KOTLIN_JVM, SurfNatsPlugin.ID)
        with(project.surfNats) {
            mapType("surf.demo.v1.Tag", "dev.slne.demo.Tag", "dev.slne.demo.TagSerializer")
            generatedVisibility.set(INTERNAL)
            protoPath.from("imports")
            compatibilityCheck.set(false)
        }
        assertEquals(
            listOf(TypeMapping("surf.demo.v1.Tag", "dev.slne.demo.Tag", "dev.slne.demo.TagSerializer")),
            project.generate.typeMappings.get(),
        )
        assertEquals(GeneratedVisibility.INTERNAL, project.generate.visibility.get())
        assertEquals(setOf(projectDir.resolve("imports")), project.generate.protoPathDirectories.files)
        assertEquals(setOf(projectDir.resolve("imports")), project.check.protoPathDirectories.files)
        assertFalse(project.check.checkEnabled.get())
    }

    @Test
    fun `compatibilityBaseline declares the baseline dependency`() {
        val project = project(KOTLIN_JVM, SurfNatsPlugin.ID)
        project.surfNats.compatibilityBaseline("dev.slne.surf.demo:demo-api:1.2.0")

        val baseline = project.configurations.getByName(SurfNatsPlugin.BASELINE_CONFIGURATION)
        val dependency = baseline.allDependencies.single()
        assertEquals("dev.slne.surf.demo:demo-api:1.2.0", "${dependency.group}:${dependency.name}:${dependency.version}")
        assertTrue(project.check.baselineConfigured.get())
    }

    @Test
    fun `wires generated sources, serialization and the check task into Kotlin JVM projects`() {
        val orders = listOf(arrayOf(KOTLIN_JVM, SurfNatsPlugin.ID), arrayOf(SurfNatsPlugin.ID, KOTLIN_JVM))
        orders.forEachIndexed { index, order ->
            val dir = projectDir.resolve(index.toString())
            val project = project(*order, dir = dir)
            val main = project.extensions.getByType(KotlinJvmProjectExtension::class.java).sourceSets.getByName("main")
            assertTrue(dir.resolve("build/generated/surf-nats/kotlin") in main.kotlin.srcDirs, order.joinToString())
            assertTrue(project.pluginManager.hasPlugin("org.jetbrains.kotlin.plugin.serialization"))

            val check = project.tasks.getByName("check")
            assertTrue(
                check.taskDependencies.getDependencies(check).any { it.name == SurfNatsPlugin.CHECK_TASK },
                "check must depend on ${SurfNatsPlugin.CHECK_TASK}",
            )
            val compile = project.tasks.getByName("compileKotlin")
            assertTrue(
                compile.taskDependencies.getDependencies(compile).any { it.name == SurfNatsPlugin.GENERATE_TASK },
                "compileKotlin must depend on ${SurfNatsPlugin.GENERATE_TASK}",
            )
        }
    }

    @Test
    fun `plugin configurations resolve JVM runtime jars without substitution of the baseline`() {
        val project = project(KOTLIN_JVM, SurfNatsPlugin.ID)
        for (name in listOf(SurfNatsPlugin.PROTO_PATH_CONFIGURATION, SurfNatsPlugin.BASELINE_CONFIGURATION)) {
            val configuration = project.configurations.getByName(name)
            assertTrue(configuration.isCanBeResolved, name)
            assertFalse(configuration.isCanBeConsumed, name)
            assertFalse(configuration.isTransitive, name)
            assertEquals(Usage.JAVA_RUNTIME, configuration.attributes.getAttribute(Usage.USAGE_ATTRIBUTE)?.name, name)
            assertEquals(KotlinPlatformType.jvm, configuration.attributes.getAttribute(KotlinPlatformType.attribute), name)
        }
        val baseline = project.configurations.getByName(SurfNatsPlugin.BASELINE_CONFIGURATION)
        assertFalse(baseline.resolutionStrategy.useGlobalDependencySubstitutionRules.get())
    }

    @Test
    fun `proto-path and baseline artifacts resolve to their extracted proto roots`() {
        val project = project(KOTLIN_JVM, SurfNatsPlugin.ID)
        val withProtos = jar("contracts.jar", "META-INF/surf-nats/proto/surf/a/v1/a.proto" to "a", "dev/A.class" to "")
        val withoutProtos = jar("other.jar", "dev/B.class" to "")
        val exploded = projectDir.resolve("classes").apply {
            resolve("META-INF/surf-nats/proto/surf/b/v1").mkdirs()
            resolve("META-INF/surf-nats/proto/surf/b/v1/b.proto").writeText("b")
        }
        project.dependencies.add(SurfNatsPlugin.PROTO_PATH_CONFIGURATION, project.files(withProtos, withoutProtos, exploded))
        project.dependencies.add(SurfNatsPlugin.BASELINE_CONFIGURATION, project.files(withProtos))

        val roots = project.generate.protoPathArchives.files.toList()
        assertEquals(listOf(listOf("surf/a/v1/a.proto"), listOf("surf/b/v1/b.proto")), roots.map { it.relativeProtos() })
        assertEquals(roots, project.check.protoPathArchives.files.toList())
        assertEquals("a", roots.first().resolve("surf/a/v1/a.proto").readText())
        assertEquals(listOf(listOf("surf/a/v1/a.proto")), project.check.baselineArchives.files.map { it.relativeProtos() })
        assertEquals(listOf(withProtos), project.check.baselineJars.files.toList())
    }

    private fun jar(name: String, vararg entries: Pair<String, String>): File {
        val file = projectDir.resolve(name)
        ZipOutputStream(file.outputStream()).use { zip ->
            for ((path, content) in entries) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return file
    }

    private fun File.relativeProtos(): List<String> =
        walkTopDown().filter { it.isFile }.map { it.relativeTo(this).invariantSeparatorsPath }.sorted().toList()

    @Test
    fun `rejects Kotlin Multiplatform projects`() {
        val error = assertFailsWith<Exception> { project("org.jetbrains.kotlin.multiplatform", SurfNatsPlugin.ID) }
        val messages = generateSequence<Throwable>(error) { it.cause }.mapNotNull { it.message }.toList()
        assertTrue(messages.any { "supports Kotlin/JVM projects only" in it }, messages.joinToString("\n"))
    }

    @Test
    fun `the surf-api bridge enables surf-nats and adds the contract dependencies`() {
        val orders = listOf(arrayOf(SURF_API_CORE, SurfNatsPlugin.ID), arrayOf(SurfNatsPlugin.ID, SURF_API_CORE))
        orders.forEachIndexed { index, order ->
            val project = project(*order, dir = projectDir.resolve(index.toString()))
            assertTrue(project.extensions.getByType(CommonSurfExtension::class.java).surfNatsEnabled.get(), order.joinToString())
            assertEquals(DefaultTypeMappings.common, project.generate.defaultTypeMappings.get())

            for (configuration in listOf("compileOnly", "testImplementation")) {
                val dependencies = project.configurations.getByName(configuration).dependencies
                    .filterIsInstance<ExternalModuleDependency>()
                    .associateBy { "${it.group}:${it.name}" }
                for (module in listOf("surf-nats-api", "surf-nats-types-common")) {
                    val dependency = assertNotNull(dependencies["dev.slne.surf.nats:$module"], "$configuration $module")
                    assertEquals(SurfNatsPluginVersion.VERSION, dependency.versionConstraint.strictVersion)
                }
                assertNull(dependencies["dev.slne.surf.nats:surf-nats-types-paper"])
                assertEquals(
                    SurfNatsPluginVersion.KOTLINX_SERIALIZATION_VERSION,
                    dependencies["org.jetbrains.kotlinx:kotlinx-serialization-protobuf"]?.version,
                )
            }
        }
    }

    @Test
    fun `paper surf-api plugins enable the paper types`() {
        val project = project(SURF_API_PAPER_RAW, SurfNatsPlugin.ID)
        assertEquals(DefaultTypeMappings.common + DefaultTypeMappings.paper, project.generate.defaultTypeMappings.get())
        val dependency = project.configurations.getByName("compileOnly").dependencies
            .filterIsInstance<ExternalModuleDependency>()
            .single { it.name == "surf-nats-types-paper" }
        assertEquals(SurfNatsPluginVersion.VERSION, dependency.versionConstraint.strictVersion)
    }

    private companion object {
        const val KOTLIN_JVM = "org.jetbrains.kotlin.jvm"
        const val SURF_API_CORE = "dev.slne.surf.api.gradle.core"
        const val SURF_API_PAPER_RAW = "dev.slne.surf.api.gradle.paper-raw"
    }
}
