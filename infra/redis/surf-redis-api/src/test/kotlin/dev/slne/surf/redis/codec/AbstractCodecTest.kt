package dev.slne.surf.redis.codec

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AbstractCodecTest {
    @Test
    fun `decoder failure does not release Redisson-owned input`() {
        val input = Unpooled.buffer().writeByte(1)
        val codec = object : AbstractCodec<Int>() {
            override fun write(buf: ByteBuf, value: Int) = buf.writeInt(value).let { }
            override fun read(buf: ByteBuf): Int = error("decode failed")
        }

        try {
            assertFailsWith<IllegalStateException> {
                codec.valueDecoder.decode(input, null)
            }
            assertEquals(1, input.refCnt())
        } finally {
            input.release()
        }
    }

    @Test
    fun `encoder failure releases its output buffer`() {
        val codec = object : AbstractCodec<Int>() {
            override fun write(buf: ByteBuf, value: Int) {
                throw IllegalArgumentException("encode failed")
            }

            override fun read(buf: ByteBuf): Int = buf.readInt()
        }

        assertFailsWith<IllegalArgumentException> {
            codec.valueEncoder.encode(1)
        }
    }
}
