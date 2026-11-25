package dev.slne.surf.redis.codec

import io.netty.buffer.ByteBuf
import io.netty.handler.codec.DecoderException

/**
 * Reads and writes a [Long] as a variable-length integer of one to [MAX_VARLONG_SIZE] bytes.
 *
 * Uses the same byte layout as [RedisVarInt]. Negative values always take ten bytes.
 */
object RedisVarLong {
    const val MAX_VARLONG_SIZE: Int = 10

    private const val DATA_BITS_MASK = 127
    private const val CONTINUATION_BIT_MASK = 128
    private const val DATA_BITS_PER_BYTE = 7

    fun getByteSize(value: Long): Int {
        for (byteSize in 1 until MAX_VARLONG_SIZE) {
            if ((value and (-1L shl (byteSize * DATA_BITS_PER_BYTE))) == 0L) {
                return byteSize
            }
        }

        return MAX_VARLONG_SIZE
    }

    fun hasContinuationBit(input: Byte): Boolean {
        return (input.toInt() and CONTINUATION_BIT_MASK) == CONTINUATION_BIT_MASK
    }

    /**
     * Reads one variable-length long from [input].
     *
     * @throws DecoderException if the value spans more than [MAX_VARLONG_SIZE] bytes.
     */
    fun read(input: ByteBuf): Long {
        var result = 0L
        var bytes = 0
        var current: Byte

        do {
            current = input.readByte()

            val shift = bytes * DATA_BITS_PER_BYTE
            result = result or ((current.toInt() and DATA_BITS_MASK).toLong() shl shift)
            bytes++

            if (bytes > MAX_VARLONG_SIZE) throw DecoderException("RedisVarLong too big")
        } while (hasContinuationBit(current))

        return result
    }

    /**
     * Writes [value] to [output] and returns [output].
     */
    fun write(output: ByteBuf, value: Long): ByteBuf {
        var remaining = value

        while ((remaining and -CONTINUATION_BIT_MASK.toLong()) != 0L) {
            val dataBits = (remaining and DATA_BITS_MASK.toLong()).toInt()

            output.writeByte(dataBits or CONTINUATION_BIT_MASK)
            remaining = remaining ushr DATA_BITS_PER_BYTE
        }

        output.writeByte(remaining.toInt())

        return output
    }
}
