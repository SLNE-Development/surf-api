package dev.slne.surf.redis.codec.default

import dev.slne.surf.redis.codec.AbstractCodec
import io.netty.buffer.ByteBuf

object IntBinaryCodec : AbstractCodec<Int>() {

    override fun write(buf: ByteBuf, value: Int) {
        buf.writeInt(value)
    }

    override fun read(buf: ByteBuf): Int {
        return buf.readInt()
    }
}