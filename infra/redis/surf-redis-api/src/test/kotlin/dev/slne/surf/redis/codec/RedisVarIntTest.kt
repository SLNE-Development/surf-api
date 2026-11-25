package dev.slne.surf.redis.codec

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.handler.codec.DecoderException
import kotlin.test.*

class RedisVarIntTest {

    @Test
    fun `writes the expected bytes for int boundaries`() {
        val expected = mapOf(
            0 to "00",
            1 to "01",
            127 to "7f",
            128 to "8001",
            300 to "ac02",
            16_383 to "ff7f",
            16_384 to "808001",
            2_097_151 to "ffff7f",
            2_097_152 to "80808001",
            Int.MAX_VALUE to "ffffffff07",
            -1 to "ffffffff0f",
            Int.MIN_VALUE to "8080808008",
        )

        for ((value, hex) in expected) {
            assertEquals(hex, encodeInt(value), "encoding of $value")
            assertEquals(hex.length / 2, RedisVarInt.getByteSize(value), "byte size of $value")
            assertEquals(value, decodeInt(hex), "decoding of $hex")
        }
    }

    @Test
    fun `writeSlow writes the same bytes as write`() {
        for (value in intSamples()) {
            val buffer = Unpooled.buffer()

            try {
                RedisVarInt.writeSlow(buffer, value)

                assertEquals(encodeInt(value), ByteBufUtil.hexDump(buffer), "encoding of $value")
            } finally {
                buffer.release()
            }
        }
    }

    @Test
    fun `round trips int samples`() {
        for (value in intSamples()) {
            assertEquals(value, decodeInt(encodeInt(value)))
            assertEquals(encodeInt(value).length / 2, RedisVarInt.getByteSize(value))
        }
    }

    @Test
    fun `rejects an int longer than five bytes`() {
        assertFailsWith<DecoderException> {
            decodeInt("ffffffffff01")
        }
    }

    @Test
    fun `writes the expected bytes for long boundaries`() {
        val expected = mapOf(
            0L to "00",
            127L to "7f",
            128L to "8001",
            Int.MAX_VALUE.toLong() to "ffffffff07",
            Long.MAX_VALUE to "ffffffffffffffff7f",
            -1L to "ffffffffffffffffff01",
            Long.MIN_VALUE to "80808080808080808001",
        )

        for ((value, hex) in expected) {
            assertEquals(hex, encodeLong(value), "encoding of $value")
            assertEquals(hex.length / 2, RedisVarLong.getByteSize(value), "byte size of $value")
            assertEquals(value, decodeLong(hex), "decoding of $hex")
        }
    }

    @Test
    fun `round trips long samples`() {
        for (shift in 0..63) {
            for (value in longArrayOf(1L shl shift, (1L shl shift) - 1, -(1L shl shift))) {
                assertEquals(value, decodeLong(encodeLong(value)))
                assertEquals(encodeLong(value).length / 2, RedisVarLong.getByteSize(value))
            }
        }
    }

    @Test
    fun `rejects a long longer than ten bytes`() {
        assertFailsWith<DecoderException> {
            decodeLong("ffffffffffffffffffff01")
        }
    }

    @Test
    fun `detects the continuation bit`() {
        assertTrue(RedisVarInt.hasContinuationBit(0x80.toByte()))
        assertFalse(RedisVarInt.hasContinuationBit(0x7f))
        assertTrue(RedisVarLong.hasContinuationBit(0xff.toByte()))
        assertFalse(RedisVarLong.hasContinuationBit(0))
    }

    private fun intSamples(): List<Int> {
        return (0..31).flatMap { shift ->
            listOf(1 shl shift, (1 shl shift) - 1, -(1 shl shift))
        }
    }

    private fun encodeInt(value: Int): String {
        return encode { RedisVarInt.write(it, value) }
    }

    private fun encodeLong(value: Long): String {
        return encode { RedisVarLong.write(it, value) }
    }

    private fun decodeInt(hex: String): Int {
        return decode(hex) { RedisVarInt.read(it) }
    }

    private fun decodeLong(hex: String): Long {
        return decode(hex) { RedisVarLong.read(it) }
    }

    private fun encode(writer: (ByteBuf) -> Unit): String {
        val buffer = Unpooled.buffer()

        try {
            writer(buffer)

            return ByteBufUtil.hexDump(buffer)
        } finally {
            buffer.release()
        }
    }

    private fun <T> decode(hex: String, reader: (ByteBuf) -> T): T {
        val buffer = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump(hex))

        try {
            val value = reader(buffer)

            assertEquals(0, buffer.readableBytes(), "unread bytes of $hex")

            return value
        } finally {
            buffer.release()
        }
    }
}
