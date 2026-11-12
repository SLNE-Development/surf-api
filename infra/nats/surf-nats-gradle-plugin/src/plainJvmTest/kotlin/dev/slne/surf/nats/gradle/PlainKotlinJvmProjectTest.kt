package dev.slne.surf.nats.gradle

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Runs on a classpath with the Kotlin Gradle plugin but without surf-api, like a plain Kotlin/JVM consumer.
 */
class PlainKotlinJvmProjectTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `applies to a Kotlin JVM project without surf-api`() {
        assertFailsWith<ClassNotFoundException> { Class.forName("dev.slne.surf.api.gradle.platform.common.CommonSurfExtension") }

        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply(SurfNatsPlugin::class.java)

        assertTrue(project.pluginManager.hasPlugin("org.jetbrains.kotlin.plugin.serialization"))
        assertNotNull(project.tasks.findByName(SurfNatsPlugin.GENERATE_TASK))
    }
}
