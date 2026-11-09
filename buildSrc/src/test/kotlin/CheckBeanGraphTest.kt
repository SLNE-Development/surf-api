import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Exercises the three faults [CheckBeanGraph] exists to find, and the parsing underneath them.
 *
 * The task reads a classpath, so every case writes real metadata files into a temporary directory
 * and hands them over as one. A case that stubbed the parsing would only test the three list
 * comprehensions and would not notice the thing most likely to break: the field names this task
 * reads out of a schema that another module owns.
 */
class CheckBeanGraphTest {

    @Test
    fun `a clean classpath passes and records what it saw`(@TempDir temp: Path) {
        val faults = run(
            temp,
            module("core", bean("CoreService", provides = listOf("CoreService", "Api"))),
            module("paper", bean("PaperService", needs = listOf("Api"))),
        )

        assertEquals(null, faults)
        assertContains(temp.resolve("report.txt").toFile().readText(), "2 beans, no faults")
    }

    @Test
    fun `the same bean class in two modules is reported with both modules`(@TempDir temp: Path) {
        val faults = failure(
            temp,
            module("core", bean("Shared")),
            module("paper", bean("Shared")),
        )

        assertContains(faults, "Shared is declared by 2 modules")
        assertContains(faults, "core.json")
        assertContains(faults, "paper.json")
    }

    @Test
    fun `two primaries for one type are reported with the type and both beans`(@TempDir temp: Path) {
        val faults = failure(
            temp,
            module(
                "core",
                bean("First", provides = listOf("First", "Api"), primary = true),
            ),
            module(
                "paper",
                bean("Second", provides = listOf("Second", "Api"), primary = true),
            ),
        )

        assertContains(faults, "Api is provided by 2 beans marked @Primary")
        assertContains(faults, "First")
        assertContains(faults, "Second")
        assertContains(faults, "@Qualifier")
    }

    @Test
    fun `a primary in one module and a plain bean in another is not a fault`(@TempDir temp: Path) {
        // This is the case @Primary exists for, and the shape every platform module has: one
        // implementation wins, the other is a fallback.
        val faults = run(
            temp,
            module("core", bean("Fallback", provides = listOf("Fallback", "Api"))),
            module("paper", bean("Chosen", provides = listOf("Chosen", "Api"), primary = true)),
        )

        assertEquals(null, faults)
    }

    @Test
    fun `a cycle across modules is reported as the chain that closes`(@TempDir temp: Path) {
        val faults = failure(
            temp,
            module("core", bean("A", provides = listOf("A"), needs = listOf("B"))),
            module("paper", bean("B", provides = listOf("B"), needs = listOf("C"))),
            module("velocity", bean("C", provides = listOf("C"), needs = listOf("A"))),
        )

        assertContains(faults, "A -> B -> C -> A")
    }

    @Test
    fun `a cycle is reported once however many beans reach it`(@TempDir temp: Path) {
        val faults = failure(
            temp,
            module(
                "core",
                bean("A", provides = listOf("A"), needs = listOf("B")),
                bean("B", provides = listOf("B"), needs = listOf("A")),
                bean("EntryOne", needs = listOf("A")),
                bean("EntryTwo", needs = listOf("B")),
            ),
        )

        assertEquals(1, faults.lines().count { it.contains("->") }, faults)
    }

    @Test
    fun `a parameter the container resolves later creates no edge`(@TempDir temp: Path) {
        // A Lazy, a collection and an optional parameter are all resolved after construction or
        // not at all, so none of them can close a cycle. Treating them as edges would report the
        // very shape that is used to break one.
        val faults = run(
            temp,
            module(
                "core",
                bean("A", provides = listOf("A"), lazyNeeds = listOf("B")),
                bean("B", provides = listOf("B"), needs = listOf("A")),
                bean("C", provides = listOf("C"), collectionNeeds = listOf("D")),
                bean("D", provides = listOf("D"), optionalNeeds = listOf("C")),
            ),
        )

        assertEquals(null, faults)
    }

    @Test
    fun `a dependency nothing provides is left to the container`(@TempDir temp: Path) {
        // Only sound for a complete runtime, and a module compiled on its own is not one.
        val faults = run(temp, module("core", bean("Needy", needs = listOf("NobodyProvidesThis"))))

        assertEquals(null, faults)
    }

    @Test
    fun `metadata without a beans array is read as no beans`(@TempDir temp: Path) {
        val classpath = Files.createDirectories(temp.resolve("empty").resolve(BEANS))
        classpath.resolve("core.json").writeText("""{"schemaVersion":1}""")

        assertEquals(null, run(temp, temp.resolve("empty")))
        assertContains(temp.resolve("report.txt").toFile().readText(), "0 beans")
    }

    /** Runs the task over [entries], returning the fault message or `null` when it passed. */
    private fun run(temp: Path, vararg entries: Path): String? {
        val project = ProjectBuilder.builder().withProjectDir(temp.toFile()).build()
        val task = project.tasks.register("checkBeanGraph", CheckBeanGraph::class.java).get()

        task.classpath.from(*entries.map { it.toFile() }.toTypedArray())
        task.report.set(temp.resolve("report.txt").toFile())

        return try {
            task.check()
            null
        } catch (failure: GradleException) {
            failure.message
        }
    }

    /** Runs the task over [entries] and asserts that it failed, returning the message. */
    private fun failure(temp: Path, vararg entries: Path): String =
        assertFailsWith<GradleException> {
            val message = run(temp, *entries)
            if (message != null) throw GradleException(message)
        }.message.orEmpty()

    /** Writes [beans] as module [name]'s metadata file and returns the classpath entry holding it. */
    private fun module(name: String, vararg beans: String): Path {
        val root = Files.createTempDirectory(name)
        root.resolve(BEANS).createDirectories()
        root.resolve(BEANS).resolve("$name.json")
            .writeText("""{"beans":[${beans.joinToString()}]}""")

        return root
    }

    /** One bean, in the shape the processor writes. */
    private fun bean(
        className: String,
        provides: List<String> = listOf(className),
        primary: Boolean = false,
        needs: List<String> = emptyList(),
        lazyNeeds: List<String> = emptyList(),
        collectionNeeds: List<String> = emptyList(),
        optionalNeeds: List<String> = emptyList(),
    ): String {
        val parameters = needs.map { """{"type":"$it"}""" } +
                lazyNeeds.map { """{"type":"$it","lazy":true}""" } +
                collectionNeeds.map { """{"type":"$it","collection":"LIST"}""" } +
                optionalNeeds.map { """{"type":"$it","optional":true}""" }

        return """
            {
              "className": "$className",
              "provides": [${provides.joinToString { "\"$it\"" }}],
              "primary": $primary,
              "ctorParams": [${parameters.joinToString()}]
            }
        """.trimIndent()
    }

    private companion object {
        const val BEANS = "META-INF/surf-api/beans"
    }
}
