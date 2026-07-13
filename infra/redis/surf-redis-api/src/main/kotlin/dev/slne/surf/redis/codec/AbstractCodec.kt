package dev.slne.surf.redis.codec

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.redisson.client.codec.BaseCodec
import org.redisson.client.protocol.Decoder
import org.redisson.client.protocol.Encoder

/**
 * Convenience base class that can be used both as a surf-redis [RedisCodec] and as a Redisson
 * codec. Subclasses only implement [write] and [read].
 *
 * Subclasses must never retain or release buffers passed to [write] or [read].
 */
abstract class AbstractCodec<T : Any> : BaseCodec(), RedisCodec<T> {
    private val encoder = Encoder { obj ->
        val buf = Unpooled.buffer()
        try {
            @Suppress("UNCHECKED_CAST")
            write(buf, obj as T)
        } catch (t: Throwable) {
            buf.release()
            throw t
        }
        buf
    }

    private val decoder = Decoder<Any> { buf, _ -> read(buf) }

    override fun getValueEncoder(): Encoder {
        return encoder
    }

    override fun getValueDecoder(): Decoder<in Any> {
        return decoder
    }

    protected abstract fun write(buf: ByteBuf, value: T)
    protected abstract fun read(buf: ByteBuf): T

    final override fun encode(buffer: ByteBuf, value: T) {
        write(buffer, value)
    }

    final override fun decode(buffer: ByteBuf): T {
        return read(buffer)
    }
}
