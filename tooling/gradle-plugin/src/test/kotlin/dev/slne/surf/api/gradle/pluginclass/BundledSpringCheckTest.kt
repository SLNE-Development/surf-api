package dev.slne.surf.api.gradle.pluginclass

import org.gradle.api.GradleException
import org.gradle.kotlin.dsl.register
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BundledSpringCheckTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `finds Spring and Spring Boot modules`() {
        val modules = listOf(
            "org.springframework.boot:spring-boot:4.0.0",
            "org.springframework:spring-core:7.0.0",
            "com.google.code.gson:gson:2.11.0",
        )

        assertEquals(
            listOf("org.springframework.boot:spring-boot:4.0.0", "org.springframework:spring-core:7.0.0"),
            BundledSpring.find(modules),
        )
    }

    @Test
    fun `ignores modules that only look like Spring`() {
        val modules = listOf(
            "org.springframework2:fake:1.0",
            "com.example:org.springframework:1.0",
            "org.springdoc:springdoc:1.0",
        )

        assertEquals(emptyList(), BundledSpring.find(modules))
    }

    @Test
    fun `finds Spring classes at the root and in multi-release directories of an archive`() {
        val artifacts = listOf(
            jar("fat-1.0.jar", "org/springframework/core/SpringVersion.class"),
            jar("versioned-1.0.jar", "META-INF/versions/21/org/springframework/core/SpringVersion.class"),
            jar("gson-2.11.0.jar", "com/google/gson/Gson.class"),
        )

        assertEquals(listOf("fat-1.0.jar", "versioned-1.0.jar"), BundledSpring.findInArtifacts(artifacts))
    }

    @Test
    fun `ignores relocated Spring, directories and files that are no archive`() {
        val classes = directory.resolve("classes").also { it.resolve("org/springframework/core").mkdirs() }
        val notes = directory.resolve("notes.txt").apply { writeText("org/springframework/") }
        val artifacts = listOf(
            jar("relocated-1.0.jar", "com/example/libs/org/springframework/core/SpringVersion.class"),
            classes,
            notes,
        )

        assertEquals(emptyList(), BundledSpring.findInArtifacts(artifacts))
    }

    private fun jar(name: String, vararg entries: String): File =
        directory.resolve(name).also { file ->
            JarOutputStream(file.outputStream()).use { jar ->
                entries.forEach { entry ->
                    jar.putNextEntry(JarEntry(entry))
                    jar.closeEntry()
                }
            }
        }

    private fun task(modules: Set<String>, artifacts: List<File> = emptyList()) =
        ProjectBuilder.builder().withProjectDir(directory.resolve("project")).build()
            .tasks.register<CheckNoBundledSpringTask>("check").get().apply {
                bundledModules.set(modules)
                bundledArtifacts.from(artifacts)
                passMarker.set(directory.resolve("passed"))
            }

    @Test
    fun `the task passes without Spring and records that it passed`() {
        val task = task(setOf("com.google.code.gson:gson:2.11.0"))

        task.check()

        assertTrue(task.passMarker.get().asFile.isFile)
    }

    @Test
    fun `the task fails with the bundled Spring modules`() {
        val failure = assertFailsWith<GradleException> {
            task(setOf("org.springframework:spring-core:7.0.0")).check()
        }

        val message = failure.message.orEmpty()
        assertTrue("org.springframework:spring-core:7.0.0" in message, message)
        assertTrue("allowBundledSpring = true" in message, message)
    }

    @Test
    fun `the task fails with an artifact that carries Spring under another coordinate`() {
        val fatJar = jar("surf-api-core-api-4.0.0-all.jar", "org/springframework/core/SpringVersion.class")

        val failure = assertFailsWith<GradleException> {
            task(setOf("dev.slne.surf.api:surf-api-core-api:4.0.0"), listOf(fatJar)).check()
        }

        val message = failure.message.orEmpty()
        assertTrue("surf-api-core-api-4.0.0-all.jar" in message, message)
        assertFalse("modules" in message, message)
    }

    @Test
    fun `a failing task records nothing`() {
        val task = task(setOf("org.springframework:spring-core:7.0.0"))

        assertFailsWith<GradleException> { task.check() }

        assertFalse(task.passMarker.get().asFile.exists())
    }
}
