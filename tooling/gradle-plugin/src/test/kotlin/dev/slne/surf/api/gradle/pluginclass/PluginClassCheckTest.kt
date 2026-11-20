package dev.slne.surf.api.gradle.pluginclass

import org.gradle.api.GradleException
import org.gradle.kotlin.dsl.register
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PluginClassCheckTest {

    @TempDir
    lateinit var directory: File

    private val paperPlugin = "dev.slne.surf.api.paper.plugin.SurfPaperPlugin"

    /**
     * Writes a class header (no members) to [root], as the compiler would.
     */
    private fun writeClass(root: File, name: String, superclass: String = "java.lang.Object") {
        val writer = ClassWriter(0)
        writer.visit(
            Opcodes.V21, Opcodes.ACC_PUBLIC, name.replace('.', '/'), null,
            superclass.replace('.', '/'), null,
        )
        writer.visitEnd()
        root.resolve("${name.replace('.', '/')}.class").apply { parentFile.mkdirs() }
            .writeBytes(writer.toByteArray())
    }

    private fun jarOf(name: String, vararg classes: Pair<String, String>): File {
        val classRoot = directory.resolve("jar-classes-$name").apply { mkdirs() }
        classes.forEach { (className, superclass) -> writeClass(classRoot, className, superclass) }

        val jar = directory.resolve("$name.jar")
        JarOutputStream(jar.outputStream()).use { output ->
            classRoot.walkTopDown().filter { it.isFile }.forEach { file ->
                output.putNextEntry(JarEntry(file.relativeTo(classRoot).invariantSeparatorsPath))
                output.write(file.readBytes())
                output.closeEntry()
            }
        }
        return jar
    }

    private fun chain(className: String, lookup: ClassPathLookup) =
        SuperclassChecker.check(className, paperPlugin, lookup)

    @Test
    fun `a direct subclass reaches the base class`() {
        val classes = directory.resolve("classes")
        writeClass(classes, "com.example.Main", paperPlugin)

        ClassPathLookup(listOf(classes), emptyList()).use { lookup ->
            assertEquals(
                SuperclassChain.Reaches(listOf("com.example.Main", paperPlugin)),
                chain("com.example.Main", lookup),
            )
        }
    }

    @Test
    fun `an indirect subclass reaches the base class through its own classes`() {
        val classes = directory.resolve("classes")
        writeClass(classes, "com.example.AbstractMain", paperPlugin)
        writeClass(classes, "com.example.Main", "com.example.AbstractMain")

        ClassPathLookup(listOf(classes), emptyList()).use { lookup ->
            val result = assertIs<SuperclassChain.Reaches>(chain("com.example.Main", lookup))
            assertEquals(listOf("com.example.Main", "com.example.AbstractMain", paperPlugin), result.chain)
        }
    }

    @Test
    fun `the chain may run through a library on the class path`() {
        val classes = directory.resolve("classes")
        writeClass(classes, "com.example.Main", "com.library.BasePlugin")
        val library = jarOf("library", "com.library.BasePlugin" to paperPlugin)

        ClassPathLookup(listOf(classes), listOf(library)).use { lookup ->
            assertIs<SuperclassChain.Reaches>(chain("com.example.Main", lookup))
        }
    }

    @Test
    fun `the chain may run through a class directory on the class path`() {
        val classes = directory.resolve("classes")
        writeClass(classes, "com.example.Main", "com.shared.AbstractPlugin")
        val sibling = directory.resolve("sibling-classes")
        writeClass(sibling, "com.shared.AbstractPlugin", paperPlugin)

        ClassPathLookup(listOf(classes), listOf(sibling)).use { lookup ->
            val result = assertIs<SuperclassChain.Reaches>(chain("com.example.Main", lookup))
            assertEquals(listOf("com.example.Main", "com.shared.AbstractPlugin", paperPlugin), result.chain)
            assertTrue(lookup.contains("com.shared.AbstractPlugin"))
        }
    }

    @Test
    fun `a class that ends at another base class misses`() {
        val classes = directory.resolve("classes")
        writeClass(classes, "com.example.Main", "org.bukkit.plugin.java.JavaPlugin")
        val bukkit = jarOf("bukkit", "org.bukkit.plugin.java.JavaPlugin" to "java.lang.Object")

        ClassPathLookup(listOf(classes), listOf(bukkit)).use { lookup ->
            val result = assertIs<SuperclassChain.Misses>(chain("com.example.Main", lookup))
            assertEquals(
                listOf("com.example.Main", "org.bukkit.plugin.java.JavaPlugin", "java.lang.Object"),
                result.chain,
            )
        }
    }

    @Test
    fun `a chain that leaves the known classes misses`() {
        val classes = directory.resolve("classes")
        writeClass(classes, "com.example.Main", "com.missing.Base")

        ClassPathLookup(listOf(classes), emptyList()).use { lookup ->
            val result = assertIs<SuperclassChain.Misses>(chain("com.example.Main", lookup))
            assertEquals(listOf("com.example.Main", "com.missing.Base"), result.chain)
        }
    }

    @Test
    fun `lookup tells present classes from absent ones`() {
        val classes = directory.resolve("classes")
        writeClass(classes, "com.example.Main")

        ClassPathLookup(listOf(classes), emptyList()).use { lookup ->
            assertTrue(lookup.contains("com.example.Main"))
            assertFalse(lookup.contains("com.example.Other"))
        }
    }

    private fun checkTask(
        expected: Map<String, String>,
        classes: File,
        classpath: List<File> = emptyList(),
    ): CheckSurfPluginClassesTask {
        val project = ProjectBuilder.builder().withProjectDir(directory.resolve("project")).build()
        return project.tasks.register<CheckSurfPluginClassesTask>("check").get().apply {
            expectedBases.set(expected)
            classDirectories.from(classes)
            compileClasspath.from(classpath)
            passMarker.set(directory.resolve("passed"))
        }
    }

    @Test
    fun `the task passes for a class that extends the base class`() {
        val classes = directory.resolve("classes")
        writeClass(classes, "com.example.Main", paperPlugin)

        checkTask(mapOf("com.example.Main" to paperPlugin), classes).check()
    }

    @Test
    fun `the task passes for a class whose base class is a class directory of another project`() {
        val classes = directory.resolve("classes")
        writeClass(classes, "com.example.Main", "com.shared.AbstractPlugin")
        val sibling = directory.resolve("sibling-classes")
        writeClass(sibling, "com.shared.AbstractPlugin", paperPlugin)

        checkTask(mapOf("com.example.Main" to paperPlugin), classes, listOf(sibling)).check()
    }

    @Test
    fun `a passing task records that it passed and a failing one does not`() {
        val classes = directory.resolve("classes")
        writeClass(classes, "com.example.Main", paperPlugin)
        writeClass(classes, "com.example.Other", "java.lang.Object")

        val passing = checkTask(mapOf("com.example.Main" to paperPlugin), classes)
        passing.check()
        assertTrue(passing.passMarker.get().asFile.isFile)

        passing.passMarker.get().asFile.delete()
        assertFailsWith<GradleException> { checkTask(mapOf("com.example.Other" to paperPlugin), classes).check() }
        assertFalse(passing.passMarker.get().asFile.exists())
    }

    @Test
    fun `the task fails for a class that does not extend the base class`() {
        val classes = directory.resolve("classes")
        writeClass(classes, "com.example.Main", "org.bukkit.plugin.java.JavaPlugin")

        val failure = assertFailsWith<GradleException> {
            checkTask(mapOf("com.example.Main" to paperPlugin), classes).check()
        }

        val message = failure.message.orEmpty()
        assertTrue("com.example.Main" in message, message)
        assertTrue("named in surfPaperPluginApi { mainClass(...) }" in message, message)
        assertTrue("must extend '$paperPlugin'" in message, message)
        assertTrue("com.example.Main -> org.bukkit.plugin.java.JavaPlugin" in message, message)
        assertTrue("verifyPluginClasses = false" in message, message)
    }

    @Test
    fun `the message points Velocity and Minestom builds at their plugin file`() {
        val classes = directory.resolve("classes")
        writeClass(classes, "com.example.Main", "java.lang.Object")

        val velocity = assertFailsWith<GradleException> {
            checkTask(mapOf("com.example.Main" to GeneratedPluginClass.VELOCITY_PLUGIN.superclass), classes).check()
        }
        val minestom = assertFailsWith<GradleException> {
            checkTask(mapOf("com.example.Missing" to GeneratedPluginClass.MINESTOM_PLUGIN.superclass), classes).check()
        }

        assertTrue("named in velocityPluginFile { main = ... }" in velocity.message.orEmpty(), velocity.message)
        assertTrue("remove the class from velocityPluginFile { main = ... }" in velocity.message.orEmpty(), velocity.message)
        assertTrue("named in minestomPluginFile { main = ... }" in minestom.message.orEmpty(), minestom.message)
        assertFalse("surf extension" in velocity.message.orEmpty() + minestom.message.orEmpty())
    }

    @Test
    fun `the task fails for a class that is not compiled`() {
        val classes = directory.resolve("classes").apply { mkdirs() }

        val failure = assertFailsWith<GradleException> {
            checkTask(mapOf("com.example.Missing" to paperPlugin), classes).check()
        }

        assertTrue("was not found among the compiled classes" in failure.message.orEmpty(), failure.message)
    }

    @Test
    fun `the task reports every failing class`() {
        val classes = directory.resolve("classes")
        writeClass(classes, "com.example.Main", "java.lang.Object")
        writeClass(classes, "com.example.Boot", "java.lang.Object")

        val failure = assertFailsWith<GradleException> {
            checkTask(
                mapOf(
                    "com.example.Main" to paperPlugin,
                    "com.example.Boot" to "dev.slne.surf.api.paper.plugin.SurfPaperBootstrapper",
                ),
                classes,
            ).check()
        }

        val message = failure.message.orEmpty()
        assertTrue("com.example.Main" in message && "com.example.Boot" in message, message)
    }
}
