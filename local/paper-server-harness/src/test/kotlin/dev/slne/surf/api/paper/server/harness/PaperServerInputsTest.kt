package dev.slne.surf.api.paper.server.harness

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.opentest4j.TestAbortedException
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.assertEquals

class PaperServerInputsTest {

    @TempDir
    lateinit var workDir: Path

    @Test
    fun `the pinned Paper jar runs when no server jar is given`() {
        val pinned = cachedDownload("paper-26.3-245.jar", "paper")

        val inputs = inputs(serverJarOverride = null, values = pinned.properties)

        assertEquals(pinned.path, inputs.serverJar())
    }

    @Test
    fun `a given server jar replaces the pinned Paper jar`() {
        val canvas = workDir.resolve("canvas-26.3.jar")
        canvas.writeText("canvas")

        val inputs = inputs(serverJarOverride = canvas, values = Properties())

        assertEquals(canvas, inputs.serverJar())
    }

    @Test
    fun `a given server jar that does not exist skips the test`() {
        val inputs = inputs(
            serverJarOverride = workDir.resolve("missing.jar"),
            values = Properties(),
        )

        assertThrows<TestAbortedException> { inputs.serverJar() }
    }

    private fun inputs(serverJarOverride: Path?, values: Properties): PaperServerInputs {
        return PaperServerInputs(
            workDir = workDir,
            downloadsDir = downloadsDir(),
            javaLauncher = workDir.resolve("java"),
            jvmArgs = emptyList(),
            serverJarOverride = serverJarOverride,
            values = values,
        )
    }

    /**
     * Places a file in the download directory whose checksum matches its pinned entry, so the
     * inputs reuse it instead of downloading.
     */
    private fun cachedDownload(fileName: String, content: String): CachedDownload {
        val path = downloadsDir().resolve(fileName)
        path.writeText(content)

        val properties = Properties()
        properties["download.paper.file"] = fileName
        properties["download.paper.url"] = "https://example.invalid/$fileName"
        properties["download.paper.sha256"] = sha256(content)

        return CachedDownload(path, properties)
    }

    private fun downloadsDir(): Path = workDir.resolve("downloads").createDirectories()

    private fun sha256(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(content.toByteArray())

        return HexFormat.of().formatHex(digest)
    }

    private class CachedDownload(val path: Path, val properties: Properties)
}
