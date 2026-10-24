package dev.slne.surf.api.gradle.platform

import dev.slne.surf.api.gradle.generated.Constants
import dev.slne.surf.api.gradle.generators.pluginfiles.VelocityPluginFile
import dev.slne.surf.api.gradle.platform.core.CoreSurfExtension
import dev.slne.surf.api.gradle.platform.core.CoreSurfPlugin
import dev.slne.surf.api.gradle.platform.paper.plugin.PaperPluginSurfExtension
import dev.slne.surf.api.gradle.platform.paper.plugin.PaperPluginSurfPlugin
import dev.slne.surf.api.gradle.platform.velocity.VelocitySurfExtension
import dev.slne.surf.api.gradle.platform.velocity.VelocitySurfPlugin
import net.minecrell.pluginyml.paper.PaperPluginDescription
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import kotlin.test.*

class SurfNatsPluginTest {

    private val natsApi = "dev.slne.surf.nats:surf-nats-api:+"
    private val standaloneApi = SurfApiPlatform.STANDALONE.dependency

    private inline fun <reified E : Any> evaluatedProject(
        plugin: Class<out Plugin<Project>>,
        configure: E.() -> Unit = {},
    ): Project {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply(plugin)
        project.extensions.getByType<E>().configure()
        (project as ProjectInternal).evaluate()
        return project
    }

    private fun Project.dependencyNotations(configuration: String) =
        configurations.getByName(configuration).dependencies
            .map { "${it.group}:${it.name}:${it.version}" }

    @Test
    fun `withSurfNats adds surf-nats-api as compileOnly and testImplementation`() {
        val project = evaluatedProject<CoreSurfExtension>(CoreSurfPlugin::class.java) {
            withSurfNats()
        }

        assertTrue(natsApi in project.dependencyNotations("compileOnly"))
        assertTrue(natsApi in project.dependencyNotations("testImplementation"))
        assertFalse(natsApi in project.dependencyNotations("implementation"))
        assertFalse(natsApi in project.dependencyNotations("api"))
    }

    @Test
    fun `withSurfNats skips testImplementation when testing is disabled`() {
        val project = evaluatedProject<CoreSurfExtension>(CoreSurfPlugin::class.java) {
            withSurfNats()
            testing.enabled.set(false)
        }

        assertTrue(natsApi in project.dependencyNotations("compileOnly"))
        assertFalse(natsApi in project.dependencyNotations("testImplementation"))
    }

    @Test
    fun `surf-nats is absent without withSurfNats`() {
        val project = evaluatedProject<CoreSurfExtension>(CoreSurfPlugin::class.java)

        assertFalse(natsApi in project.dependencyNotations("compileOnly"))
        assertFalse(project.extensions.getByType<CoreSurfExtension>().surfNatsEnabled.get())
    }

    @Test
    fun `surfNatsEnabled reflects withSurfNats`() {
        val project = evaluatedProject<CoreSurfExtension>(CoreSurfPlugin::class.java) {
            withSurfNats()
        }

        assertTrue(project.extensions.getByType<CoreSurfExtension>().surfNatsEnabled.get())
    }

    @Test
    fun `core tests get the standalone runtime`() {
        val project = evaluatedProject<CoreSurfExtension>(CoreSurfPlugin::class.java)

        assertTrue(standaloneApi in project.dependencyNotations("testRuntimeOnly"))
    }

    @Test
    fun `core without testing gets no standalone runtime`() {
        val project = evaluatedProject<CoreSurfExtension>(CoreSurfPlugin::class.java) {
            testing.enabled.set(false)
        }

        assertFalse(standaloneApi in project.dependencyNotations("testRuntimeOnly"))
    }

    @Test
    fun `return value checker is enabled`() {
        val project = evaluatedProject<CoreSurfExtension>(CoreSurfPlugin::class.java)

        val args = project.extensions.getByType<KotlinJvmProjectExtension>()
            .compilerOptions.freeCompilerArgs.get()
        assertTrue("-Xreturn-value-checker=check" in args)
    }

    @Test
    fun `paper plugin requires surf-nats-paper`() {
        val project =
            evaluatedProject<PaperPluginSurfExtension>(PaperPluginSurfPlugin::class.java) {
                mainClass("dev.slne.test.TestPlugin")
                withSurfNats()
            }

        val description = project.extensions.getByType<PaperPluginDescription>()
        for (dependencies in listOf(description.bootstrapDependencies, description.serverDependencies)) {
            val nats = assertNotNull(dependencies.findByName("surf-nats-paper"))
            assertEquals(true, nats.required)
        }
    }

    @Test
    fun `paper plugin without withSurfNats does not require surf-nats-paper`() {
        val project =
            evaluatedProject<PaperPluginSurfExtension>(PaperPluginSurfPlugin::class.java) {
                mainClass("dev.slne.test.TestPlugin")
            }

        val description = project.extensions.getByType<PaperPluginDescription>()
        assertEquals(null, description.serverDependencies.findByName("surf-nats-paper"))
        assertEquals(null, description.bootstrapDependencies.findByName("surf-nats-paper"))
    }

    @Test
    fun `velocity plugin depends on surf-nats-velocity`() {
        val project = evaluatedProject<VelocitySurfExtension>(VelocitySurfPlugin::class.java) {
            withSurfNats()
        }

        val nats = project.extensions.getByType<VelocityPluginFile>()
            .pluginDependencies.getByName("surf-nats-velocity")
        assertTrue(nats.enabled.get())
        assertFalse(nats.optional.get())
    }

    @Test
    fun `velocity plugin without withSurfNats leaves surf-nats-velocity disabled`() {
        val project = evaluatedProject<VelocitySurfExtension>(VelocitySurfPlugin::class.java)

        val nats = project.extensions.getByType<VelocityPluginFile>()
            .pluginDependencies.getByName("surf-nats-velocity")
        assertFalse(nats.enabled.get())
    }

    @Test
    fun `luckperms version is the modrinth plugin version`() {
        assertTrue(Constants.LUCKPERMS_VERSION.endsWith("-bukkit"), Constants.LUCKPERMS_VERSION)
    }
}
