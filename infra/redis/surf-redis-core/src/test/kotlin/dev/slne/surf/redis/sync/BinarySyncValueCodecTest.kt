package dev.slne.surf.redis.sync

import dev.slne.surf.redis.codec.RedisCodec
import dev.slne.surf.redis.codec.RedisCodecException
import io.netty.buffer.ByteBuf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BinarySyncValueCodecTest {
    @Test
    fun `round trips binary values through the Lua-safe representation`() {
        val codec = BinarySyncValueCodec(IntCodec, "SyncValue 'counter' value")

        val encoded = codec.encode(42)

        assertTrue(encoded.none { it == '\u0000' })
        assertEquals(42, codec.decode(encoded))
    }

    @Test
    fun `codec descriptor is stable and versioned`() {
        val codec = BinarySyncValueCodec(IntCodec, "test")
        assertEquals("surf-codec-v1:8:test-int:3", codec.descriptor)
    }

    @Test
    fun `decode rejects unread trailing bytes with context`() {
        val codec = BinarySyncValueCodec(object : RedisCodec<Int> {
            override fun encode(buffer: ByteBuf, value: Int) {
                buffer.writeInt(value)
                buffer.writeByte(1)
            }

            override fun decode(buffer: ByteBuf): Int = buffer.readInt()
        }, "SyncList 'numbers' element")

        val failure = assertFailsWith<RedisCodecException> {
            codec.decode(codec.encode(5))
        }
        assertTrue(failure.message.orEmpty().contains("SyncList 'numbers' element"))
    }

    private object IntCodec : RedisCodec<Int> {
        override val codecId = "test-int"
        override val version = 3

        override fun encode(buffer: ByteBuf, value: Int) {
            buffer.writeInt(value)
        }

        override fun decode(buffer: ByteBuf): Int = buffer.readInt()
    }
}
