package dev.slne.surf.redis.codec

import io.netty.buffer.Unpooled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RedisUtf8StringTest {
    @Test
    fun `round trips ascii and multi-byte text without a temporary buffer`() {
        val buffer = Unpooled.buffer()
        try {
            buffer.writeString("surf-redis")
            buffer.writeString("Greetings 🌊")

            assertEquals("surf-redis", buffer.readString())
            assertEquals("Greetings 🌊", buffer.readString())
            assertEquals(0, buffer.readableBytes())
        } finally {
            buffer.release()
        }
    }

    @Test
    fun `enforces character limit before writing`() {
        val buffer = Unpooled.buffer()
        try {
            assertFailsWith<Exception> {
                buffer.writeString("too long", maxLength = 3)
            }
            assertEquals(0, buffer.writerIndex())
        } finally {
            buffer.release()
        }
    }

    @Test
    fun `uses actual utf8 size instead of reserving worst case capacity`() {
        val value = "a".repeat(1024)
        val encodedSize = 2 + value.length // two-byte VarInt length prefix plus ASCII payload
        val buffer = Unpooled.buffer(encodedSize, encodedSize * 3)
        try {
            buffer.writeString(value, maxLength = value.length)

            assertEquals(encodedSize, buffer.writerIndex())
            assertEquals(encodedSize, buffer.capacity())
        } finally {
            buffer.release()
        }
    }

    @Test
    fun `container readers reject attacker-controlled allocation sizes`() {
        val buffer = Unpooled.buffer()
        try {
            buffer.writeVarInt(Int.MAX_VALUE)
            assertFailsWith<Exception> {
                buffer.readVarIntArray()
            }
        } finally {
            buffer.release()
        }
    }
}
