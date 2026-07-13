package dev.slne.surf.redis.codec.default

import dev.slne.surf.redis.codec.AbstractCodec
import dev.slne.surf.redis.codec.readString
import dev.slne.surf.redis.codec.writeString
import io.netty.buffer.ByteBuf

object StringBinaryCodec : AbstractCodec<String>() {

    override fun write(buf: ByteBuf, value: String) {
        buf.writeString(value)
    }

    override fun read(buf: ByteBuf): String {
        return buf.readString()
    }
}