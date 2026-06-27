package dev.slne.surf.redis.codec

import io.netty.buffer.ByteBuf

object RedisVarLong {

    const val MAX_VARLONG_SIZE: Int = 10

    private const val DATA_BITS_MASK: Long = 0x7F
    private const val CONTINUATION_BIT_MASK: Int = 0x80
    private const val DATA_BITS_PER_BYTE: Int = 7

    @JvmStatic
    fun getByteSize(value: Long): Int {
        for (i in 1 until MAX_VARLONG_SIZE) {
            if ((value and (-1L shl (i * DATA_BITS_PER_BYTE))) == 0L) {
                return i
            }
        }

        return MAX_VARLONG_SIZE
    }

    @JvmStatic
    fun hasContinuationBit(input: Byte): Boolean {
        return (input.toInt() and CONTINUATION_BIT_MASK) == CONTINUATION_BIT_MASK
    }

    @JvmStatic
    fun read(input: ByteBuf): Long {
        var out = 0L
        var bytes = 0

        var byte: Byte
        do {
            byte = input.readByte()

            out = out or ((byte.toLong() and DATA_BITS_MASK) shl (bytes++ * DATA_BITS_PER_BYTE))

            checkDecoding(bytes <= MAX_VARLONG_SIZE) { "RedisVarLong too big" }
        } while (hasContinuationBit(byte))

        return out
    }

    @JvmStatic
    fun write(output: ByteBuf, value: Long): ByteBuf {
        var remaining = value

        while ((remaining and DATA_BITS_MASK.inv()) != 0L) {
            output.writeByte(((remaining and DATA_BITS_MASK).toInt()) or CONTINUATION_BIT_MASK)
            remaining = remaining ushr DATA_BITS_PER_BYTE
        }

        output.writeByte(remaining.toInt())
        return output
    }
}