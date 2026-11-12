package dev.slne.surf.redis.codec.default

import dev.slne.surf.redis.codec.AbstractCodec
import dev.slne.surf.redis.codec.readVarInt
import dev.slne.surf.redis.codec.writeVarInt
import io.netty.buffer.ByteBuf

object VarIntBinaryCodec : AbstractCodec<Int>() {

    override fun write(buf: ByteBuf, value: Int) {
        buf.writeVarInt(value)
    }

    override fun read(buf: ByteBuf): Int {
        return buf.readVarInt()
    }
}