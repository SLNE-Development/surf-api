package dev.slne.surf.redis.codec.default

import dev.slne.surf.redis.codec.AbstractCodec
import io.netty.buffer.ByteBuf

object LongBinaryCodec : AbstractCodec<Long>() {

    override fun write(buf: ByteBuf, value: Long) {
        buf.writeLong(value)
    }

    override fun read(buf: ByteBuf): Long {
        return buf.readLong()
    }
}