package dev.slne.surf.redis.codec.default

import dev.slne.surf.redis.codec.AbstractCodec
import io.netty.buffer.ByteBuf

object DoubleBinaryCodec: AbstractCodec<Double>() {

    override fun write(buf: ByteBuf, value: Double) {
        buf.writeDouble(value)
    }

    override fun read(buf: ByteBuf): Double {
        return buf.readDouble()
    }
}