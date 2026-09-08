package dev.slne.surf.redis.codec;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class RedisVarInt {
    public static final int MAX_VARINT_SIZE = 5;

    private static final int DATA_BITS_MASK = 127;
    private static final int CONTINUATION_BIT_MASK = 128;
    private static final int DATA_BITS_PER_BYTE = 7;

    private static final int[] VARINT_EXACT_BYTE_LENGTHS = new int[Integer.SIZE + 1];

    private RedisVarInt() {
        throw new AssertionError("No instances");
    }

    static {
        for (int i = 0; i <= Integer.SIZE; ++i) {
            VARINT_EXACT_BYTE_LENGTHS[i] = (int) Math.ceil(((Integer.SIZE - 1) - (i - 1)) / (double) DATA_BITS_PER_BYTE);
        }
        VARINT_EXACT_BYTE_LENGTHS[32] = 1; // Special case for the number 0.
    }

    public static int getByteSize(final int value) {
        return VARINT_EXACT_BYTE_LENGTHS[Integer.numberOfLeadingZeros(value)];
    }

    public static boolean hasContinuationBit(final byte in) {
        return (in & CONTINUATION_BIT_MASK) == CONTINUATION_BIT_MASK;
    }

    public static int read(final ByteBuf input) {
        int out = 0;
        int bytes = 0;

        byte in;
        do {
            in = input.readByte();
            out |= (in & (byte) DATA_BITS_MASK) << bytes++ * DATA_BITS_PER_BYTE;
            if (bytes > MAX_VARINT_SIZE) {
                throw new DecoderException("RedisVarInt too big");
            }
        } while (hasContinuationBit(in));

        return out;
    }

    public static ByteBuf write(final ByteBuf output, int value) {
        if ((value & (0xFFFFFFFF << DATA_BITS_PER_BYTE)) == 0) {
            output.writeByte(value);
        } else if ((value & (0xFFFFFFFF << (DATA_BITS_PER_BYTE * 2))) == 0) {
            final int s = (value & DATA_BITS_MASK | CONTINUATION_BIT_MASK) << Byte.SIZE | (value >>> DATA_BITS_PER_BYTE);
            output.writeShort(s);
        } else {
            writeSlow(output, value);
        }
        return output;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static ByteBuf writeSlow(final ByteBuf output, int value) {
        while ((value & -CONTINUATION_BIT_MASK) != 0) {
            output.writeByte(value & DATA_BITS_MASK | CONTINUATION_BIT_MASK);
            value >>>= DATA_BITS_PER_BYTE;
        }

        output.writeByte(value);
        return output;
    }
}
