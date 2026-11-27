package dev.slne.surf.api.paper.server.impl.inventory.temporary

import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.*
import kotlin.test.*

/**
 * Checks the stored form of temporary inventory sessions over opaque per-slot bytes, without a
 * server.
 */
class TemporaryInventoryFormatTest {

    @Test
    fun `a session survives a round trip`() {
        val session = session(
            slots = listOf(bytes(1, 2, 3), bytes(4), bytes(5, 6)),
            selectedSlot = 4,
            state = bytes(9, 8, 7),
        )

        val decoded = roundTrip(StoredEntry(listOf(session), emptyList())).sessions.single()

        assertEquals(session.id, decoded.id)
        assertEquals("ProtectPlugin", decoded.owner)
        assertEquals("protect:region_edit", decoded.key)
        assertEquals(4, decoded.selectedSlot)
        assertSlots(listOf(bytes(1, 2, 3), bytes(4), bytes(5, 6)), decoded.slots)
        assertContentEquals(bytes(9, 8, 7), decoded.state)
    }

    @Test
    fun `empty slots stay empty`() {
        val session = session(slots = listOf(EMPTY, bytes(1), EMPTY, EMPTY))

        val decoded = roundTrip(StoredEntry(listOf(session), emptyList())).sessions.single()

        assertSlots(listOf(EMPTY, bytes(1), EMPTY, EMPTY), decoded.slots)

        val restored = decoded.planRestore(slotCount = 4) { it.first().toInt() }
        assertEquals(listOf(null, 1, null, null), restored.items)
        assertTrue(restored.unreadable.isEmpty())
    }

    @Test
    fun `fewer stored slots than the server has leave the extra slots empty`() {
        val session = session(slots = listOf(bytes(1), bytes(2)))

        val restored = roundTrip(StoredEntry(listOf(session), emptyList()))
            .sessions
            .single()
            .planRestore(slotCount = 4) { it.first().toInt() }

        assertEquals(listOf(1, 2, null, null), restored.items)
        assertTrue(restored.unreadable.isEmpty())
    }

    @Test
    fun `more stored slots than the server has keep the extra items stored`() {
        val session = session(slots = listOf(bytes(1), bytes(2), EMPTY, bytes(4)))

        val restored = roundTrip(StoredEntry(listOf(session), emptyList()))
            .sessions
            .single()
            .planRestore(slotCount = 2) { it.first().toInt() }

        assertEquals(listOf(1, 2), restored.items)
        assertEquals(listOf(3), restored.unreadable.map { it.slot })
        assertContentEquals(bytes(4), restored.unreadable.single().bytes)
    }

    @Test
    fun `an unreadable slot stays stored after the session ends`() {
        val session = session(slots = listOf(bytes(1), bytes(UNREADABLE), bytes(3)))
        val entry = roundTrip(StoredEntry(listOf(session), emptyList()))

        val restored = entry.sessions.single().planRestore(slotCount = 3, read = ::readOrFail)
        val remaining = roundTrip(entry.withoutSession(session.id, restored.unreadable))

        assertEquals(listOf(1, null, 3), restored.items)
        assertTrue(remaining.sessions.isEmpty())

        val retained = remaining.retainedSlots.single()
        assertEquals(session.id, retained.sessionId)
        assertEquals(1, retained.slot)
        assertContentEquals(bytes(UNREADABLE), retained.bytes)
    }

    @Test
    fun `retained slots survive the next session`() {
        val retained = RetainedSlot(UUID.randomUUID(), slot = 7, bytes = bytes(UNREADABLE))
        val next = session(slots = listOf(bytes(1)))

        val decoded = roundTrip(StoredEntry(listOf(next), listOf(retained)))

        assertEquals(next.id, decoded.sessions.single().id)
        assertEquals(7, decoded.retainedSlots.single().slot)
    }

    @Test
    fun `an entry without sessions and retained slots is empty`() {
        val session = session(slots = listOf(bytes(1)))
        val entry = StoredEntry(listOf(session), emptyList())

        assertTrue(entry.withoutSession(session.id, emptyList()).isEmpty)
    }

    @Test
    fun `a newer format version is reported, not read`() {
        val newer = ByteArrayOutputStream().also { buffer ->
            DataOutputStream(buffer).use { output ->
                output.writeInt(TemporaryInventoryFormat.VERSION + 1)
                output.writeUTF("written by a later surf-api")
            }
        }.toByteArray()

        val decoded = TemporaryInventoryFormat.decode(newer)

        assertIs<DecodedEntry.UnknownVersion>(decoded)
        assertEquals(TemporaryInventoryFormat.VERSION + 1, decoded.version)
    }

    @Test
    fun `a stored session is found by its id`() {
        val session = session(slots = listOf(bytes(1)))
        val entry = StoredEntry(listOf(session), emptyList())

        assertEquals(session.id, entry.session(session.id)?.id)
        assertNull(entry.session(UUID.randomUUID()))
    }

    private fun roundTrip(entry: StoredEntry): StoredEntry {
        val decoded = TemporaryInventoryFormat.decode(TemporaryInventoryFormat.encode(entry))

        return assertIs<DecodedEntry.Supported>(decoded).entry
    }

    private fun assertSlots(expected: List<ByteArray>, actual: List<ByteArray>) {
        assertEquals(expected.size, actual.size)

        for ((index, slot) in expected.withIndex()) {
            assertContentEquals(slot, actual[index], "slot $index")
        }
    }

    private fun readOrFail(bytes: ByteArray): Int {
        require(bytes.first() != UNREADABLE) { "Unknown item" }

        return bytes.first().toInt()
    }

    private companion object {

        const val UNREADABLE: Byte = -1

        val EMPTY = ByteArray(0)

        fun bytes(vararg values: Byte): ByteArray = values

        fun session(
            slots: List<ByteArray>,
            selectedSlot: Int = 0,
            state: ByteArray = EMPTY,
        ): StoredSession {
            return StoredSession(
                id = UUID.randomUUID(),
                owner = "ProtectPlugin",
                key = "protect:region_edit",
                selectedSlot = selectedSlot,
                slots = slots,
                state = state,
            )
        }
    }
}
