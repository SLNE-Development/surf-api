package dev.slne.surf.api.core.server.component.property

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the one distinction the container depends on: an absent file is not a broken one.
 *
 * `@ConditionalOnProperty` decides whether a bean is created at all, and it is allowed to fall back
 * to `matchIfMissing` when a property is not set. So "not set" has to mean not set. If a file that
 * exists but cannot be parsed also reads as "not set", a server comes up with every guarded feature
 * on whichever side its default happens to be, and nothing says why — which is what this used to
 * do, with a logged warning nobody reads.
 *
 * The cache is cleared around every case: it holds entries for 30 minutes and is process-wide, so
 * one case would otherwise answer the next.
 */
class PropertyServiceTest {

    @BeforeTest
    @AfterTest
    fun clearCache() = PropertyService.clearCache()

    @Test
    fun `a property is read from the default file`(@TempDir temp: Path) {
        (temp / "properties.yml").writeText("test:\n  property: \"true\"\n")

        assertEquals("true", PropertyService.getProperty(temp, arrayOf("test", "property")))
        assertTrue(PropertyService.hasProperty(temp, arrayOf("test", "property")))
    }

    @Test
    fun `no file means the property is not set`(@TempDir temp: Path) {
        assertNull(PropertyService.getProperty(temp, arrayOf("test", "property")))
        assertTrue(!PropertyService.hasProperty(temp, arrayOf("test", "property")))
    }

    @Test
    fun `a file without the key means the property is not set`(@TempDir temp: Path) {
        (temp / "properties.yml").writeText("something:\n  else: 1\n")

        assertNull(PropertyService.getProperty(temp, arrayOf("test", "property")))
    }

    @Test
    fun `a directory where the file should be means the property is not set`(@TempDir temp: Path) {
        // Not a regular file, so there is nothing to read and nothing broken either.
        (temp / "properties.yml").createDirectories()

        assertNull(PropertyService.getProperty(temp, arrayOf("test", "property")))
    }

    @Test
    fun `a file that exists and cannot be parsed fails instead of reading as unset`(
        @TempDir temp: Path,
    ) {
        (temp / "properties.yml").writeText("test:\n  property: \"unterminated\n   - [\n")

        val failure = assertFailsWith<IllegalStateException> {
            PropertyService.getProperty(temp, arrayOf("test", "property"))
        }

        assertContains(failure.message.orEmpty(), "properties.yml")
        assertContains(failure.message.orEmpty(), "Fix the file")
    }

    @Test
    fun `a failed load is not cached, so fixing the file is enough`(@TempDir temp: Path) {
        val file = temp / "properties.yml"
        file.writeText("test:\n  property: \"unterminated\n   - [\n")

        assertFailsWith<IllegalStateException> {
            PropertyService.getProperty(temp, arrayOf("test", "property"))
        }

        // The point of letting it throw rather than caching an empty node: an operator corrects
        // the file and the next read answers, without a restart or a cache call.
        file.writeText("test:\n  property: \"true\"\n")

        assertEquals("true", PropertyService.getProperty(temp, arrayOf("test", "property")))
    }

    @Test
    fun `a custom file name is read instead of the default`(@TempDir temp: Path) {
        (temp / "other.yml").writeText("test:\n  property: \"from-other\"\n")
        (temp / "properties.yml").writeText("test:\n  property: \"from-default\"\n")

        assertEquals(
            "from-other",
            PropertyService.getProperty(temp, arrayOf("test", "property"), "other.yml"),
        )
    }
}
