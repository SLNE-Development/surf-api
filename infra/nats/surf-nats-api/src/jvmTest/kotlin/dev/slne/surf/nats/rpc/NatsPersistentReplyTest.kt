package dev.slne.surf.nats.rpc

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class NatsPersistentReplyTest {

    @Test
    fun `stored replies are restored with their kind and bytes`() {
        val replies = listOf(
            NatsPersistentReply.empty(),
            NatsPersistentReply.value(byteArrayOf(8, 42)),
            NatsPersistentReply.unknownAnswer(),
            NatsPersistentReply.error(IllegalStateException("broken")),
        )

        for (reply in replies) {
            val restored = NatsPersistentReply.restore(reply.format, reply.bytes)
            assertEquals(reply.kind, restored.kind)
            assertContentEquals(reply.bytes, restored.bytes)
        }
    }

    @Test
    fun `error reply carries the exception type and message`() {
        val reply = NatsPersistentReply.error(IllegalArgumentException("first line\nsecond line"))

        assertEquals(IllegalArgumentException::class.java.name, reply.errorType)
        assertEquals("first line\nsecond line", reply.errorMessage)
        assertNull(NatsPersistentReply.value(ByteArray(0)).errorType)
    }

    @Test
    fun `error message is truncated and a missing message is empty`() {
        assertEquals(4096, NatsPersistentReply.error(IllegalStateException("x".repeat(5000))).errorMessage!!.length)
        assertEquals("", NatsPersistentReply.error(IllegalStateException()).errorMessage)
    }

    @Test
    fun `invalid stored replies are rejected`() {
        assertFailsWith<IllegalArgumentException> { NatsPersistentReply.restore("surf-nats.value.v0", ByteArray(0)) }
        assertFailsWith<IllegalArgumentException> {
            NatsPersistentReply.restore(NatsPersistentReply.empty().format, byteArrayOf(1))
        }
        assertFailsWith<IllegalArgumentException> {
            NatsPersistentReply.restore(NatsPersistentReply.unknownAnswer().format, byteArrayOf(1))
        }
        assertFailsWith<IllegalArgumentException> {
            NatsPersistentReply.restore(NatsPersistentReply.error(IllegalStateException()).format, "no separator".encodeToByteArray())
        }
    }
}
