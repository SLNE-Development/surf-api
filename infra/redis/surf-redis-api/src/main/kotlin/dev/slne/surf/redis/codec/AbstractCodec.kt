package dev.slne.surf.redis.codec

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.redisson.client.codec.BaseCodec
import org.redisson.client.protocol.Decoder
import org.redisson.client.protocol.Encoder

abstract class AbstractCodec<T : Any> : BaseCodec() {
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

    private val decoder = Decoder<Any> { buf, _ ->
        try {
            read(buf)
        } catch (t: Throwable) {
            buf.release()
            throw t
        }
    }

    override fun getValueEncoder(): Encoder? {
        return encoder
    }

    override fun getValueDecoder(): Decoder<in Any>? {
        return decoder
    }

    protected abstract fun write(buf: ByteBuf, value: T)
    protected abstract fun read(buf: ByteBuf): T
}