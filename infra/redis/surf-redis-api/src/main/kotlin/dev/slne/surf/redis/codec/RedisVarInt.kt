package dev.slne.surf.redis.codec

import io.netty.buffer.ByteBuf
import kotlin.math.ceil

object RedisVarInt {
    const val MAX_VARINT_SIZE = 5

    private const val DATA_BITS_MASK: Int = 127
    private const val CONTINUATION_BIT_MASK: Int = 128
    private const val DATA_BITS_PER_BYTE: Int = 7

    private val VARINT_EXACT_BYTE_LENGTHS = IntArray(Int.SIZE_BITS + 1).also { lengths ->
        for (i in 0..Int.SIZE_BITS) {
            lengths[i] = ceil(((Int.SIZE_BITS - 1).toDouble() - (i - 1)) / DATA_BITS_PER_BYTE).toInt()
        }

        // Special case for the number 0.
        lengths[Int.SIZE_BITS] = 1
    }

    @JvmStatic
    fun getByteSize(value: Int): Int {
        return VARINT_EXACT_BYTE_LENGTHS[Integer.numberOfLeadingZeros(value)]
    }

    @JvmStatic
    fun hasContinuationBit(input: Byte): Boolean {
        return (input.toInt() and CONTINUATION_BIT_MASK) == CONTINUATION_BIT_MASK
    }


    @JvmStatic
    fun read(input: ByteBuf): Int {
        var out = 0
        var bytes = 0

        var byte: Byte
        do {
            byte = input.readByte()

            out = out or ((byte.toInt() and DATA_BITS_MASK) shl (bytes++ * DATA_BITS_PER_BYTE))

            checkDecoding(bytes <= MAX_VARINT_SIZE) { "RedisVarInt too big" }
        } while (hasContinuationBit(byte))

        return out
    }

    @JvmStatic
    fun write(output: ByteBuf, value: Int): ByteBuf {
        if ((value and (-1 shl DATA_BITS_PER_BYTE)) == 0) {
            output.writeByte(value)
        } else if ((value and (-1 shl (DATA_BITS_PER_BYTE * 2))) == 0) {
            val packed =
                (((value and DATA_BITS_MASK) or CONTINUATION_BIT_MASK) shl Byte.SIZE_BITS) or
                        (value ushr DATA_BITS_PER_BYTE)

            output.writeShort(packed)
        } else {
            writeSlow(output, value)
        }

        return output
    }

    @JvmStatic
    fun writeSlow(output: ByteBuf, value: Int): ByteBuf {
        var remaining = value

        while ((remaining and CONTINUATION_BIT_MASK.inv()) != 0) {
            output.writeByte((remaining and DATA_BITS_MASK) or CONTINUATION_BIT_MASK)
            remaining = remaining ushr DATA_BITS_PER_BYTE
        }

        output.writeByte(remaining)
        return output
    }
}