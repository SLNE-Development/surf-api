package dev.slne.surf.redis.sync

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull

class RemoteMutationScriptTest {
    private val xaddCall =
        "redis.call('XADD', streamKey, 'MAXLEN', '~', maxLen, '*', fieldType, eventType, fieldMsg, msg)"
    private val msgAssignment = "local msg = tostring(ver) .. delim .. originId .. delim .. payload"

    @Test
    fun `remote mutation scripts return version and stream payload`() {
        val scripts = listOf(
            "/lua/sync/value/compare-and-set.lua",
            "/lua/sync/value/get-and-set.lua",
            "/lua/sync/map/put-remote.lua",
            "/lua/sync/map/put-if-absent.lua",
            "/lua/sync/map/remove-remote.lua",
            "/lua/sync/list/append-remote.lua",
            "/lua/sync/list/set-at-remote.lua",
            "/lua/sync/list/remove-at-remote.lua",
        )

        for (path in scripts) {
            val script = read(path)

            assertContains(script, xaddCall, message = path)
            assertContains(script, msgAssignment, message = path)
            assertContains(script, "return { ver, payload", message = path)
        }
    }

    @Test
    fun `remote mutation scripts mirror the payload of their local counterparts`() {
        assertSamePayload(
            local = "/lua/sync/map/put.lua",
            remote = "/lua/sync/map/put-remote.lua",
            payload = "local payload = key .. delim .. value",
        )
        assertSamePayload(
            local = "/lua/sync/map/put.lua",
            remote = "/lua/sync/map/put-if-absent.lua",
            payload = "local payload = key .. delim .. value",
        )
        assertSamePayload(
            local = "/lua/sync/map/remove.lua",
            remote = "/lua/sync/map/remove-remote.lua",
            payload = "local payload = key .. delim .. old",
        )
        assertSamePayload(
            local = "/lua/sync/list/append.lua",
            remote = "/lua/sync/list/append-remote.lua",
            payload = "local payload = tostring(idx) .. delim .. value",
        )
        assertSamePayload(
            local = "/lua/sync/value/set.lua",
            remote = "/lua/sync/value/compare-and-set.lua",
            payload = "local payload = element",
            remotePayload = "local payload = newValue",
        )
        assertSamePayload(
            local = "/lua/sync/value/set.lua",
            remote = "/lua/sync/value/get-and-set.lua",
            payload = "local payload = element",
            remotePayload = "local payload = newValue",
        )
    }

    private fun assertSamePayload(
        local: String,
        remote: String,
        payload: String,
        remotePayload: String = payload,
    ) {
        assertContains(read(local), payload, message = local)
        assertContains(read(remote), remotePayload, message = remote)
    }

    private fun read(path: String): String {
        val stream = assertNotNull(javaClass.getResourceAsStream(path), path)
        return stream.bufferedReader().use { it.readText() }
    }
}
