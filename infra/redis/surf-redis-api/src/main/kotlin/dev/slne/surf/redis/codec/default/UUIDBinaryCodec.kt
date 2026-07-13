package dev.slne.surf.redis.codec.default

import dev.slne.surf.redis.codec.AbstractCodec
import dev.slne.surf.redis.codec.writeUuid
import io.netty.buffer.ByteBuf
import java.util.*

object UUIDBinaryCodec : AbstractCodec<UUID>() {
    override fun write(buf: ByteBuf, value: UUID) {
        buf.writeUuid(value)
    }

    override fun read(buf: ByteBuf): UUID {
        return UUID(buf.readLong(), buf.readLong())
    }
}