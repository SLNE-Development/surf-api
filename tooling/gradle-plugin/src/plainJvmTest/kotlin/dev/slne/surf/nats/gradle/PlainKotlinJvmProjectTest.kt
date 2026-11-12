package dev.slne.surf.nats.gradle

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Applies the plugin to a plain Kotlin/JVM project, like a consumer that uses none of the surf-api platform plugins.
 */
class PlainKotlinJvmProjectTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `applies to a Kotlin JVM project without a surf-api platform plugin`() {
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply(SurfNatsPlugin::class.java)

        assertTrue(project.pluginManager.hasPlugin("org.jetbrains.kotlin.plugin.serialization"))
        assertNotNull(project.tasks.findByName(SurfNatsPlugin.GENERATE_TASK))
    }
}
