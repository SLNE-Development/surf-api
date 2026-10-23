package dev.slne.surf.nats.gradle.tasks

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProtoArchivesTest {

    @TempDir
    lateinit var root: File

    private fun jar(name: String, entries: Map<String, String>): File {
        val file = root.resolve(name)
        ZipOutputStream(file.outputStream()).use { zip ->
            for ((path, content) in entries) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return file
    }

    private fun File.relativeFiles(): List<String> =
        walkTopDown().filter { it.isFile }.map { it.relativeTo(this).invariantSeparatorsPath }.sorted().toList()

    /**
     * Extracts [archive] into `out/<name>` and returns that root, or `null` when the archive requested none.
     */
    private fun extract(archive: File): File? {
        var target: File? = null
        val copied = ProtoArchives.extract(archive) { root.resolve("out/${archive.name}").also { target = it } }
        assertEquals(copied > 0, target != null, archive.name)
        return target
    }

    @Test
    fun `extracts only the packed protos of a jar`() {
        val jar = jar(
            "first.jar",
            mapOf(
                "META-INF/surf-nats/proto/surf/a/v1/a.proto" to "a",
                "META-INF/surf-nats/proto/readme.txt" to "ignored",
                "surf/elsewhere/v1/b.proto" to "ignored",
                "dev/slne/A.class" to "ignored",
            ),
        )

        val extracted = extract(jar)!!

        assertEquals(listOf("surf/a/v1/a.proto"), extracted.relativeFiles())
        assertEquals("a", extracted.resolve("surf/a/v1/a.proto").readText())
    }

    @Test
    fun `archives without packed protos request no root`() {
        assertEquals(null, extract(jar("empty.jar", mapOf("dev/slne/B.class" to "ignored"))))
        assertEquals(null, extract(root.resolve("missing")))
        assertFalse(root.resolve("out").exists())
    }

    @Test
    fun `reads directories as exploded jars`() {
        val classes = root.resolve("classes")
        classes.resolve("META-INF/surf-nats/proto/surf/a/v1").mkdirs()
        classes.resolve("META-INF/surf-nats/proto/surf/a/v1/a.proto").writeText("a")
        classes.resolve("META-INF/surf-nats/proto/surf/a/v1/notes.md").writeText("ignored")

        assertEquals(listOf("surf/a/v1/a.proto"), extract(classes)!!.relativeFiles())
    }

    @Test
    fun `rejects entries that escape the proto root`() {
        val evil = jar("evil.jar", mapOf("META-INF/surf-nats/proto/../../../evil.proto" to "evil"))
        val error = assertFailsWith<IllegalArgumentException> { ProtoArchives.extract(evil) { root.resolve("out") } }
        assertTrue("escapes the proto root" in error.message.orEmpty(), error.message)
    }
}
