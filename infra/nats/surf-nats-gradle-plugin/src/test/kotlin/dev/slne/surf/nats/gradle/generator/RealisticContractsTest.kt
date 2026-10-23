package dev.slne.surf.nats.gradle.generator

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Runs the generator over the multi-file proto set in `src/test/resources/realistic` and compares selected outputs
 * with the snapshots in `src/test/resources/snapshots`.
 *
 * On a mismatch the actual output is written to `build/snapshots-actual/` for review.
 */
class RealisticContractsTest {
    @TempDir
    lateinit var temp: File

    private val sources = File(javaClass.getResource("/realistic")!!.toURI())
    private val mappings = listOf(TypeMapping("surf.demo.v1.Tag", "dev.slne.surf.demo.Tag", "dev.slne.surf.demo.TagSerializer"))

    private fun input() =
        GeneratorInput(listOf(sources), emptyList(), mappings, GeneratedVisibility.PUBLIC, defaultTypeMappings = DefaultTypeMappings.common)

    private fun generate(outputDir: File = temp.resolve("out")): Map<String, String> {
        val result = SurfNatsGenerator().generate(input(), outputDir)
        return result.files.associate { it.relativeTo(outputDir).invariantSeparatorsPath to it.readText() }
    }

    @Test
    fun `generates one Kotlin file per proto file in the Kotlin package`() {
        assertEquals(
            listOf(
                "dev/slne/surf/demo/proto/v1/Common.kt",
                "dev/slne/surf/demo/proto/v1/Events.kt",
                "dev/slne/surf/demo/proto/v1/PunishService.kt",
                "dev/slne/surf/demo/proto/v2/Versioned.kt",
            ),
            generate().keys.toList(),
        )
    }

    @Test
    fun `punish service output matches the snapshot`() {
        assertSnapshot("PunishService.kt", generate().getValue("dev/slne/surf/demo/proto/v1/PunishService.kt"))
    }

    @Test
    fun `events output matches the snapshot`() {
        assertSnapshot("Events.kt", generate().getValue("dev/slne/surf/demo/proto/v1/Events.kt"))
    }

    @Test
    fun `common output matches the snapshot`() {
        assertSnapshot("Common.kt", generate().getValue("dev/slne/surf/demo/proto/v1/Common.kt"))
    }

    @Test
    fun `versioned output matches the snapshot`() {
        assertSnapshot("Versioned.kt", generate().getValue("dev/slne/surf/demo/proto/v2/Versioned.kt"))
    }

    @Test
    fun `two runs produce identical bytes`() {
        val first = SurfNatsGenerator().generate(input(), temp.resolve("first"))
        val second = SurfNatsGenerator().generate(input(), temp.resolve("second"))
        assertEquals(
            first.files.map { it.relativeTo(temp.resolve("first")).invariantSeparatorsPath },
            second.files.map { it.relativeTo(temp.resolve("second")).invariantSeparatorsPath },
        )
        first.files.zip(second.files).forEach { (a, b) -> assertContentEquals(a.readBytes(), b.readBytes(), a.name) }
    }

    @Test
    fun `stale files are removed from the output directory`() {
        val outputDir = temp.resolve("out")
        val stale = outputDir.resolve("dev/slne/Stale.kt").apply { parentFile.mkdirs(); writeText("stale") }
        generate(outputDir)
        assertEquals(false, stale.exists())
    }

    private fun assertSnapshot(name: String, actual: String) {
        val expected = javaClass.getResource("/snapshots/$name.txt")?.readText()?.replace("\r\n", "\n")
        if (expected != actual) {
            val target = File("build/snapshots-actual/$name.txt")
            target.parentFile.mkdirs()
            target.writeText(actual)
            if (expected == null) fail("snapshot $name is missing; actual output written to ${target.absolutePath}")
            assertEquals(expected, actual, "snapshot $name differs; actual output written to ${target.absolutePath}")
        }
    }
}
