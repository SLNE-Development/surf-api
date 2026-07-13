package dev.slne.surf.redis.codec.default

import dev.slne.surf.redis.codec.AbstractCodec
import dev.slne.surf.redis.codec.readVarLong
import dev.slne.surf.redis.codec.writeVarLong
import io.netty.buffer.ByteBuf

object VarLongBinaryCodec: AbstractCodec<Long>() {

    override fun write(buf: ByteBuf, value: Long) {
        buf.writeVarLong(value)
    }

    override fun read(buf: ByteBuf): Long {
        return buf.readVarLong()
    }
}