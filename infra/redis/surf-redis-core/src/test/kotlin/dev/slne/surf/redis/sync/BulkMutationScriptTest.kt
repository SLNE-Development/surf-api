package dev.slne.surf.redis.sync

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull

class BulkMutationScriptTest {
    @Test
    fun `bulk mutation scripts return their contiguous version range`() {
        val scripts = listOf(
            "/lua/sync/list/remove-many.lua",
            "/lua/sync/set/remove-many.lua",
            "/lua/sync/map/remove-many.lua"
        )

        for (path in scripts) {
            val stream = assertNotNull(javaClass.getResourceAsStream(path), path)
            val script = stream.bufferedReader().use { it.readText() }

            assertContains(script, "local firstVersion")
            assertContains(script, "return {firstVersion, lastVersion}")
        }
    }
}
