package dev.slne.surf.redis.codec

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import java.nio.charset.StandardCharsets

object RedisUtf8String {
    const val MAX_STRING_LENGTH: Int = 32_767

    @JvmStatic
    fun read(input: ByteBuf, maxLength: Int = MAX_STRING_LENGTH): String {
        val maxEncodedLength = ByteBufUtil.utf8MaxBytes(maxLength)
        val bufferLength = input.readVarInt()

        checkDecoding(bufferLength >= 0) { "The received encoded string buffer length is less than zero" }
        checkDecoding(bufferLength <= maxEncodedLength) { "The received encoded string buffer length is longer than maximum allowed ($bufferLength > $maxEncodedLength)" }

        val availableBytes = input.readableBytes()

        checkDecoding(bufferLength <= availableBytes) { "Not enough bytes in buffer, expected $bufferLength, but got $availableBytes" }


        val result = input.toString(input.readerIndex(), bufferLength, StandardCharsets.UTF_8)
        input.readerIndex(input.readerIndex() + bufferLength)

        checkDecoding(result.length <= maxLength) { "The received string length is longer than maximum allowed (${result.length} > $maxLength)" }

        return result
    }

    @JvmStatic
    fun write(output: ByteBuf, value: CharSequence, maxLength: Int = MAX_STRING_LENGTH) {
        checkEncoding(value.length <= maxLength) { "String too big (was ${value.length} characters, max $maxLength)" }

        val bytesWritten = ByteBufUtil.utf8Bytes(value)
        val maxAllowedEncodedLength = ByteBufUtil.utf8MaxBytes(maxLength)

        checkEncoding(bytesWritten <= maxAllowedEncodedLength) { "String too big (was $bytesWritten bytes encoded, max $maxAllowedEncodedLength)" }

        output.writeVarInt(bytesWritten)
        ByteBufUtil.reserveAndWriteUtf8(output, value, bytesWritten)
    }
}
