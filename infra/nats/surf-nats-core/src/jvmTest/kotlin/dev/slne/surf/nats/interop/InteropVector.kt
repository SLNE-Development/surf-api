package dev.slne.surf.nats.interop

import dev.slne.surf.nats.codec.NatsCodec
import kotlinx.serialization.KSerializer
import java.util.*

/**
 * A Kotlin value that the JVM encodes for the interop script and the value it expects back.
 *
 * [echo] is what the JVM must decode when the script sends the message back through `InteropService`; `null` means
 * the script must reject the Kotlin bytes and sends nothing back. A [raw] vector is served by [RawVectorResponder]
 * because the JS-facing message type differs from the Kotlin type that encodes it.
 */
internal class InteropVector(
    val name: String,
    private val serializer: KSerializer<*>,
    val value: Any,
    val echo: Any? = value,
    val raw: Boolean = false,
) {
    @Suppress("UNCHECKED_CAST")
    fun encode(): ByteArray = NatsCodec.encode(serializer as KSerializer<Any>, value)

    fun hex(): String = HexFormat.of().formatHex(encode())
}
