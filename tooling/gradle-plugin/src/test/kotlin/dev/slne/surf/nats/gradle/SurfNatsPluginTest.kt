package dev.slne.surf.nats.gradle

import com.squareup.wire.schema.ProtoType
import dev.slne.surf.api.gradle.generators.pluginfiles.VelocityPluginFile
import dev.slne.surf.nats.gradle.generator.DefaultTypeMappings
import dev.slne.surf.nats.gradle.generator.GeneratedVisibility
import dev.slne.surf.nats.gradle.generator.TypeMapping
import dev.slne.surf.nats.gradle.tasks.CheckSurfNatsCompatibilityTask
import dev.slne.surf.nats.gradle.tasks.GenerateSurfNatsKotlinTask
import dev.slne.weaver.gradle.WeaverExtension
import net.minecrell.pluginyml.paper.PaperPluginDescription
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.*

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
        assertNotNull(project.tasks.findByName(SurfNatsPlugin.STAGE_TASK))
        assertNotNull(project.tasks.findByName(SurfNatsPlugin.BASELINE_TASK))
        assertTrue(project.pluginManager.hasPlugin("dev.slne.weaver"))
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
        assertFalse(project.check.active.get())
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
            listOf(TypeMapping(ProtoType.get("surf.demo.v1.Tag"), "dev.slne.demo.Tag", "dev.slne.demo.TagSerializer")),
            project.generate.typeMappings.get(),
        )
        assertEquals(GeneratedVisibility.INTERNAL, project.generate.visibility.get())
        assertEquals(setOf(projectDir.resolve("imports")), project.generate.protoPathDirectories.files)
        assertEquals(setOf(projectDir.resolve("imports")), project.check.protoPathDirectories.files)
        assertFalse(project.check.checkEnabled.get())
    }

    @Test
    fun `compatibilityBaseline sets the baseline version and activates the check`() {
        val project = project(KOTLIN_JVM, SurfNatsPlugin.ID)
        project.surfNats.compatibilityBaseline("1.2.0")

        assertEquals("1.2.0", project.surfNats.compatibilityBaseline.get())
        assertTrue(project.check.active.get())
    }

    @Test
    fun `weaver repositories replace the default lookup repository`() {
        val project = project(KOTLIN_JVM, SurfNatsPlugin.ID)
        val weaver = project.extensions.getByType(WeaverExtension::class.java)
        assertEquals(listOf("proto-releases-public"), weaver.repositories.get())

        weaver.repositories("private-protos")
        assertEquals(listOf("private-protos"), weaver.repositories.get())
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
    fun `rejects Kotlin Multiplatform projects`() {
        val error = assertFailsWith<Exception> { project("org.jetbrains.kotlin.multiplatform", SurfNatsPlugin.ID) }
        val messages = generateSequence<Throwable>(error) { it.cause }.mapNotNull { it.message }.toList()
        assertTrue(messages.any { "supports Kotlin/JVM projects only" in it }, messages.joinToString("\n"))
    }

    @Test
    fun `surf-api plugins get the contract dependencies`() {
        val orders = listOf(arrayOf(SURF_API_CORE, SurfNatsPlugin.ID), arrayOf(SurfNatsPlugin.ID, SURF_API_CORE))
        orders.forEachIndexed { index, order ->
            val project = project(*order, dir = projectDir.resolve(index.toString()))
            assertEquals(DefaultTypeMappings.common, project.generate.defaultTypeMappings.get())

            for (configuration in listOf("compileOnly", "testImplementation")) {
                val dependencies = project.configurations.getByName(configuration).dependencies
                    .filterIsInstance<ExternalModuleDependency>()
                    .associateBy { "${it.group}:${it.name}" }
                for (module in listOf("surf-api-nats-api", "surf-api-nats-types-common")) {
                    val dependency = assertNotNull(dependencies["dev.slne.surf.api:$module"], "$configuration $module")
                    assertEquals(SurfNatsPluginVersion.VERSION, dependency.versionConstraint.strictVersion)
                }
                assertNull(dependencies["dev.slne.surf.api:surf-api-nats-types-paper"])
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
            .single { it.name == "surf-api-nats-types-paper" }
        assertEquals(SurfNatsPluginVersion.VERSION, dependency.versionConstraint.strictVersion)
    }

    @Test
    fun `paper plugins require surf-api-nats-paper at bootstrap and server`() {
        val orders = listOf(arrayOf(SURF_API_PAPER_PLUGIN, SurfNatsPlugin.ID), arrayOf(SurfNatsPlugin.ID, SURF_API_PAPER_PLUGIN))
        orders.forEachIndexed { index, order ->
            val project = project(*order, dir = projectDir.resolve(index.toString()))
            val description = project.extensions.getByType(PaperPluginDescription::class.java)
            for (dependencies in listOf(description.bootstrapDependencies, description.serverDependencies)) {
                val dependency = assertNotNull(dependencies.findByName("surf-api-nats-paper"), order.joinToString())
                assertTrue(dependency.required)
                assertTrue(dependency.joinClasspath)
                assertEquals(PaperPluginDescription.RelativeLoadOrder.BEFORE, dependency.load)
            }
        }
    }

    @Test
    fun `velocity plugins require surf-api-nats-velocity`() {
        val orders = listOf(arrayOf(SURF_API_VELOCITY, SurfNatsPlugin.ID), arrayOf(SurfNatsPlugin.ID, SURF_API_VELOCITY))
        orders.forEachIndexed { index, order ->
            val project = project(*order, dir = projectDir.resolve(index.toString()))
            val dependency = project.extensions.getByType(VelocityPluginFile::class.java)
                .pluginDependencies.findByName("surf-api-nats-velocity")
            assertFalse(assertNotNull(dependency, order.joinToString()).optional.get())
        }
    }

    @Test
    fun `standalone projects become microservices with the NATS runtime`() {
        val orders = listOf(
            arrayOf(SURF_API_STANDALONE, SurfNatsPlugin.ID),
            arrayOf(SurfNatsPlugin.ID, SURF_API_STANDALONE),
        )
        orders.forEachIndexed { index, order ->
            val project = project(*order, dir = projectDir.resolve(index.toString()))
            assertTrue(project.pluginManager.hasPlugin(MICROSERVICE), order.joinToString())

            val runtime = project.configurations.getByName("implementation").dependencies
                .filterIsInstance<ExternalModuleDependency>()
                .single { it.group == "dev.slne.surf.api" && it.name == "surf-api-nats-standalone" }
            assertEquals(SurfNatsPluginVersion.VERSION, runtime.versionConstraint.strictVersion)
        }
    }

    @Test
    fun `core projects do not become microservices`() {
        val project = project(SURF_API_CORE, SurfNatsPlugin.ID)

        assertFalse(project.pluginManager.hasPlugin(MICROSERVICE))
        val runtime = project.configurations.getByName("implementation").dependencies
            .find { it.group == "dev.slne.surf.api" && it.name == "surf-api-nats-standalone" }
        assertNull(runtime)
    }

    private companion object {
        const val KOTLIN_JVM = "org.jetbrains.kotlin.jvm"
        const val MICROSERVICE = "dev.slne.surf.api.gradle.microservice"
        const val SURF_API_STANDALONE = "dev.slne.surf.api.gradle.standalone"
        const val SURF_API_CORE = "dev.slne.surf.api.gradle.core"
        const val SURF_API_PAPER_PLUGIN = "dev.slne.surf.api.gradle.paper-plugin"
        const val SURF_API_PAPER_RAW = "dev.slne.surf.api.gradle.paper-raw"
        const val SURF_API_VELOCITY = "dev.slne.surf.api.gradle.velocity"
    }
}
