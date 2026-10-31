package dev.slne.surf.api.gradle.platform

import dev.slne.surf.api.gradle.generated.Constants
import dev.slne.surf.api.gradle.platform.core.CoreSurfExtension
import dev.slne.surf.api.gradle.platform.core.CoreSurfPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoreSurfPluginTest {

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
    fun `luckperms version is the modrinth plugin version`() {
        assertTrue(Constants.LUCKPERMS_VERSION.endsWith("-bukkit"), Constants.LUCKPERMS_VERSION)
    }
}
