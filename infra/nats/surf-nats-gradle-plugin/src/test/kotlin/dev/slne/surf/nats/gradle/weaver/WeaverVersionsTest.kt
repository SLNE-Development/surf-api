package dev.slne.surf.nats.gradle.weaver

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeaverVersionsTest {

    @Test
    fun `orders numeric parts numerically and releases above qualified versions`() {
        assertTrue(WeaverVersions.compare("1.10.0", "1.9.0") > 0)
        assertTrue(WeaverVersions.compare("1.4.0", "1.4.0-rc.1") > 0)
        assertTrue(WeaverVersions.compare("1.4.0-rc.10", "1.4.0-rc.2") > 0)
        assertEquals(0, WeaverVersions.compare("1.4", "1.4.0"))
    }

    @Test
    fun `previous release is the highest release strictly below the project version`() {
        val versions = listOf("0.9.0", "1.0.0", "1.3.0", "1.4.0-rc.1", "1.4.0", "2.0.0-SNAPSHOT")
        assertEquals("0.9.0", WeaverVersions.previousRelease(versions, "1.0.0-SNAPSHOT"))
        assertEquals("1.4.0-rc.1", WeaverVersions.previousRelease(versions, "1.4.0-rc.2"))
        assertEquals("1.4.0", WeaverVersions.previousRelease(versions, "2.0.0-SNAPSHOT"))
        assertNull(WeaverVersions.previousRelease(listOf("2.0.0", "2.1.0"), "2.0.0"))
    }
}
