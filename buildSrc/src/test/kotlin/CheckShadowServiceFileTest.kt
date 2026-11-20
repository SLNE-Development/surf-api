import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class CheckShadowServiceFileTest {

    @Test
    fun `reports nothing when the file lists every provider`() {
        val jar = jarWith("META-INF/services/demo.Service" to "first.Impl\nsecond.Impl\n")

        assertEquals(emptyList(), missingServiceProviders(jar, "demo.Service", listOf("first.Impl", "second.Impl")))
    }

    @Test
    fun `reports the providers a copy of the file lost`() {
        val jar = jarWith("META-INF/services/demo.Service" to "first.Impl\n")

        assertEquals(listOf("second.Impl"), missingServiceProviders(jar, "demo.Service", listOf("first.Impl", "second.Impl")))
    }

    @Test
    fun `ignores comments and blank lines`() {
        val jar = jarWith("META-INF/services/demo.Service" to "# first\n\nfirst.Impl # trailing\n")

        assertEquals(emptyList(), missingServiceProviders(jar, "demo.Service", listOf("first.Impl")))
    }

    @Test
    fun `reports every provider when the jar has no such service file`() {
        val jar = jarWith("META-INF/services/demo.Other" to "first.Impl\n")

        assertEquals(listOf("first.Impl"), missingServiceProviders(jar, "demo.Service", listOf("first.Impl")))
    }

    private fun jarWith(vararg entries: Pair<String, String>): File {
        val file = File(createTempDirectory("service-file-check").toFile().apply { deleteOnExit() }, "test.jar")
        file.deleteOnExit()
        ZipOutputStream(file.outputStream()).use { zip ->
            for ((name, content) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return file
    }
}
