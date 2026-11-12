package dev.slne.surf.nats.gradle.weaver

import dev.slne.surf.nats.gradle.SurfNatsPlugin
import dev.slne.surf.nats.gradle.SurfNatsPluginVersion
import dev.slne.weaver.gradle.FetchProtosTask
import dev.slne.weaver.gradle.PublishProtosTask
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PublishManifestTypeDependenciesTest {

    @TempDir
    lateinit var projectDir: File

    private val common = "${SurfNatsPluginVersion.GROUP}:surf-nats-types-common:${SurfNatsPluginVersion.VERSION}"
    private val paper = "${SurfNatsPluginVersion.GROUP}:surf-nats-types-paper:${SurfNatsPluginVersion.VERSION}"

    private fun project(vararg plugins: String, dir: File = projectDir): Project {
        val project = ProjectBuilder.builder().withProjectDir(dir)
            .withGradleUserHomeDir(File("build/tmp/projectBuilderUserHome")).build()
        for (plugin in plugins) {
            if (plugin == SurfNatsPlugin.ID) project.pluginManager.apply(SurfNatsPlugin::class.java)
            else project.pluginManager.apply(plugin)
        }
        return project
    }

    private val Project.manifestDependencies: List<String>
        get() = (tasks.getByName("publishProtos") as PublishProtosTask).dependencies.get()

    private val Project.weaverConfigurationDependencies: List<String>
        get() = configurations.getByName("weaver").dependencies.map { "${it.group}:${it.name}:${it.version}" }

    @Test
    fun `a plain project publishes the common types as a dependency`() {
        val project = project(SurfNatsPlugin.ID)

        assertEquals(listOf(common), project.manifestDependencies)
    }

    @Test
    fun `paper projects also publish the paper types as a dependency`() {
        val plugins = listOf("dev.slne.surf.api.gradle.paper-plugin", "dev.slne.surf.api.gradle.paper-raw")
        val orders = plugins.flatMap { listOf(listOf(it, SurfNatsPlugin.ID), listOf(SurfNatsPlugin.ID, it)) }
        orders.forEachIndexed { index, order ->
            val project = project(*order.toTypedArray(), dir = projectDir.resolve(index.toString()))

            assertEquals(listOf(common, paper), project.manifestDependencies, order.toString())
        }
    }

    @Test
    fun `non-paper surf-api projects publish the common types only`() {
        val project = project("dev.slne.surf.api.gradle.core", SurfNatsPlugin.ID)

        assertEquals(listOf(common), project.manifestDependencies)
    }

    @Test
    fun `the weaver configuration and fetchProtos never contain the automatic types dependencies`() {
        val project = project("dev.slne.surf.api.gradle.paper-raw", SurfNatsPlugin.ID)

        assertEquals(listOf(common, paper), project.manifestDependencies)
        assertEquals(emptyList(), project.weaverConfigurationDependencies)
        assertEquals(emptyList(), (project.tasks.getByName("fetchProtos") as FetchProtosTask).dependencies.get())
    }

    @Test
    fun `declared weaver dependencies join the manifest in front of the automatic ones`() {
        val project = project(SurfNatsPlugin.ID)
        project.dependencies.add("weaver", "dev.slne.demo:other:2.0.0")

        assertEquals(listOf("dev.slne.demo:other:2.0.0", common), project.manifestDependencies)
        assertEquals(listOf("dev.slne.demo:other:2.0.0"), project.weaverConfigurationDependencies)
    }

    @Test
    fun `a types module declared by the user replaces the automatic version instead of duplicating it`() {
        val project = project(SurfNatsPlugin.ID)
        project.dependencies.add("weaver", "${SurfNatsPluginVersion.GROUP}:surf-nats-types-common:9.9.9")

        val dependencies = project.manifestDependencies

        assertEquals(listOf("${SurfNatsPluginVersion.GROUP}:surf-nats-types-common:9.9.9"), dependencies)
        assertTrue(common !in dependencies)
    }
}
