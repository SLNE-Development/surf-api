package dev.slne.surf.redis.codec

import org.redisson.client.codec.StringCodec
import org.redisson.client.protocol.Decoder
import java.nio.charset.Charset
import java.util.*

/**
 * Redisson codec that stores a [UUID] in its string form, such as
 * `550e8400-e29b-41d4-a716-446655440000`.
 *
 * Values are encoded like [StringCodec] and decoded back into [UUID] instances.
 */
open class UUIDCodec : StringCodec {
    private val decoder = Decoder<Any> { buffer, state ->
        val string = super.getValueDecoder().decode(buffer, state) as String

        UUID.fromString(string)
    }

    constructor() : super()

    /**
     * Creates a codec for [classLoader]; Redisson calls it reflectively when it copies a codec.
     *
     * @param classLoader the class loader to decode with, or `null` for the default one.
     */
    constructor(classLoader: ClassLoader?) : super(classLoader)

    constructor(charset: Charset) : super(charset)

    constructor(charsetName: String) : super(charsetName)

    override fun getValueDecoder(): Decoder<Any> = decoder

    companion object {
        val INSTANCE: UUIDCodec = UUIDCodec()
    }
}
