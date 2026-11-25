package dev.slne.surf.redis.codec

import io.netty.buffer.ByteBuf
import io.netty.handler.codec.DecoderException

/**
 * Reads and writes an [Int] as a variable-length integer of one to [MAX_VARINT_SIZE] bytes.
 *
 * Each byte carries seven data bits, least significant group first, and sets its high bit when
 * another byte follows. Negative values always take five bytes.
 */
object RedisVarInt {
    const val MAX_VARINT_SIZE: Int = 5

    private const val DATA_BITS_MASK = 127
    private const val CONTINUATION_BIT_MASK = 128
    private const val DATA_BITS_PER_BYTE = 7

    private val byteSizeByLeadingZeros = IntArray(Int.SIZE_BITS + 1) { leadingZeros ->
        val significantBits = Int.SIZE_BITS - leadingZeros
        val byteSize = (significantBits + DATA_BITS_PER_BYTE - 1) / DATA_BITS_PER_BYTE

        byteSize.coerceAtLeast(1)
    }

    fun getByteSize(value: Int): Int {
        return byteSizeByLeadingZeros[Integer.numberOfLeadingZeros(value)]
    }

    fun hasContinuationBit(input: Byte): Boolean {
        return (input.toInt() and CONTINUATION_BIT_MASK) == CONTINUATION_BIT_MASK
    }

    /**
     * Reads one variable-length integer from [input].
     *
     * @throws DecoderException if the value spans more than [MAX_VARINT_SIZE] bytes.
     */
    fun read(input: ByteBuf): Int {
        var result = 0
        var bytes = 0
        var current: Byte

        do {
            current = input.readByte()

            val shift = bytes * DATA_BITS_PER_BYTE
            result = result or ((current.toInt() and DATA_BITS_MASK) shl shift)
            bytes++

            if (bytes > MAX_VARINT_SIZE) throw DecoderException("RedisVarInt too big")
        } while (hasContinuationBit(current))

        return result
    }

    /**
     * Writes [value] to [output] and returns [output].
     */
    fun write(output: ByteBuf, value: Int): ByteBuf {
        if ((value and (-1 shl DATA_BITS_PER_BYTE)) == 0) {
            output.writeByte(value)
        } else if ((value and (-1 shl (DATA_BITS_PER_BYTE * 2))) == 0) {
            val firstByte = (value and DATA_BITS_MASK) or CONTINUATION_BIT_MASK
            val secondByte = value ushr DATA_BITS_PER_BYTE

            output.writeShort((firstByte shl Byte.SIZE_BITS) or secondByte)
        } else {
            writeSlow(output, value)
        }

        return output
    }

    /**
     * Writes [value] to [output] one byte at a time and returns [output].
     */
    fun writeSlow(output: ByteBuf, value: Int): ByteBuf {
        var remaining = value

        while ((remaining and -CONTINUATION_BIT_MASK) != 0) {
            output.writeByte((remaining and DATA_BITS_MASK) or CONTINUATION_BIT_MASK)
            remaining = remaining ushr DATA_BITS_PER_BYTE
        }

        output.writeByte(remaining)

        return output
    }
}
