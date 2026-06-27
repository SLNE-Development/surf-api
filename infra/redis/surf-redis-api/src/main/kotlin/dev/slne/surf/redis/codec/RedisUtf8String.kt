package dev.slne.surf.redis.codec

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.handler.codec.DecoderException
import io.netty.handler.codec.EncoderException
import java.nio.charset.StandardCharsets

object RedisUtf8String {
    const val MAX_STRING_LENGTH: Int = 32_767

    @JvmStatic
    fun read(input: ByteBuf, maxLength: Int = MAX_STRING_LENGTH): String {
        val maxEncodedLength = ByteBufUtil.utf8MaxBytes(maxLength)
        val bufferLength = input.readVarInt()

        if (bufferLength > maxEncodedLength) {
            throw DecoderException(
                "The received encoded string buffer length is longer than maximum allowed ($bufferLength > $maxEncodedLength)"
            )
        }

        if (bufferLength < 0) {
            throw DecoderException("The received encoded string buffer length is less than zero")
        }

        val availableBytes = input.readableBytes()

        if (bufferLength > availableBytes) {
            throw DecoderException("Not enough bytes in buffer, expected $bufferLength, but got $availableBytes")
        }

        val result = input.toString(input.readerIndex(), bufferLength, StandardCharsets.UTF_8)
        input.readerIndex(input.readerIndex() + bufferLength)

        if (result.length > maxLength) {
            throw DecoderException(
                "The received string length is longer than maximum allowed (${result.length} > $maxLength)"
            )
        }

        return result
    }

    @JvmStatic
    fun write(output: ByteBuf, value: CharSequence, maxLength: Int = MAX_STRING_LENGTH) {
        if (value.length > maxLength) {
            throw EncoderException("String too big (was ${value.length} characters, max $maxLength)")
        }

        val maxEncodedValueLength = ByteBufUtil.utf8MaxBytes(value)
        val tmp = output.alloc().buffer(maxEncodedValueLength)

        try {
            val bytesWritten = ByteBufUtil.writeUtf8(tmp, value)
            val maxAllowedEncodedLength = ByteBufUtil.utf8MaxBytes(maxLength)

            if (bytesWritten > maxAllowedEncodedLength) {
                throw EncoderException(
                    "String too big (was $bytesWritten bytes encoded, max $maxAllowedEncodedLength)"
                )
            }

            output.writeVarInt(bytesWritten)
            output.writeBytes(tmp)
        } finally {
            tmp.release()
        }
    }
}